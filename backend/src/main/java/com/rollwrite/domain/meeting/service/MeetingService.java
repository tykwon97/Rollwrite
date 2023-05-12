package com.rollwrite.domain.meeting.service;

import com.rollwrite.domain.meeting.dto.*;
import com.rollwrite.domain.meeting.entity.*;
import com.rollwrite.domain.meeting.repository.*;
import com.rollwrite.domain.question.entity.Question;
import com.rollwrite.domain.question.repository.AnswerRepository;
import com.rollwrite.domain.question.repository.QuestionRepository;
import com.rollwrite.domain.user.dto.FindUserResDto;
import com.rollwrite.domain.user.entity.User;
import com.rollwrite.domain.user.repository.UserRepository;

import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final AsyncMeetingService asyncMeetingService;

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AwardRepository awardRepository;
    private final AnswerRepository answerRepository;
    private final MeetingRepository meetingRepository;
    private final QuestionRepository questionRepository;
    private final TagMeetingRepository tagMeetingRepository;
    private final ParticipantRepository participantRepository;

    @Value("${inviteUrl}")
    private String baseUrl;

    @Transactional
    public AddMeetingResDto addMeeting(Long userId,
                                       AddMeetingReqDto addMeetingReqDto) throws NoSuchAlgorithmException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 초대 코드 생성
        // TODO: SecureRandom 서버에서는 작동 제대로 안 함 -> 수정 필요
//        SecureRandom random = SecureRandom.getInstanceStrong();
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        byte[] codeBytes = new byte[15];
        random.nextBytes(codeBytes);
        String inviteCode = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);

        // Meeting 생성
        Meeting meeting = Meeting.builder()
                .addMeetingReqDto(addMeetingReqDto)
                .inviteCode(inviteCode)
                .build();
        meetingRepository.save(meeting);

        // tag id에 해당하는 Meeting(tagMeetingList)에 추가
        List<TagDto> tagList = new ArrayList<>();
        List<TagMeeting> tagMeetingList = tagIdToTagMeetingList(
                meeting, addMeetingReqDto.getTag(), tagList);
        meeting.updateTagMeetingList(tagMeetingList);

        // 질문에 사용 될 Tag
        String tag = "";
        for (TagDto tagDto : tagList) {
            tag += tagDto.getContent() + ",";
        }

        // 날짜 계산
        long period = ChronoUnit.DAYS.between(meeting.getStartDay(), meeting.getEndDay());

        // Chat GPT 생성 질문 10개 저장
        asyncMeetingService.saveGptQuestion(tag, meeting, period);

        // Meeting 생성자 Meeting에 추가
        Participant participant = Participant.builder()
                .user(user)
                .meeting(meeting)
                .build();
        participantRepository.save(participant);

        return AddMeetingResDto.builder()
                .meeting(meeting)
                .tag(tagList)
                .inviteUrl(baseUrl + inviteCode)
                .build();
    }

    private List<TagMeeting> tagIdToTagMeetingList(Meeting meeting, List<Long> tagIds,
                                                   List<TagDto> tagList) {
        List<TagMeeting> tagMeetingList = new ArrayList<>();
        for (Long id : tagIds) {
            // tag id에 해당하는 tag 찾기
            Tag tag = tagRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 태그를 찾을 수 없습니다"));

            // Tag -> TagDto
            tagList.add(TagDto.of(tag));

            // TagMeeting 에 추가
            TagMeeting tagMeeting = TagMeeting.builder()
                    .tag(tag)
                    .meeting(meeting)
                    .build();
            tagMeetingRepository.save(tagMeeting);

            tagMeetingList.add(tagMeeting);
        }
        return tagMeetingList;
    }

    @Transactional
    public int joinMeeting(Long userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Optional<Meeting> optionalMeeting = meetingRepository.validMeetingInviteCode(inviteCode);
        if (!optionalMeeting.isPresent()) {
            return 1;
        }
        Meeting meeting = optionalMeeting.get();

        Optional<Participant> isExistedUser = participantRepository.findByMeetingAndUser(meeting, user);
        if (isExistedUser.isPresent()) {
            return 2;
        } else {
            Participant participant = Participant.builder()
                    .user(user)
                    .meeting(meeting)
                    .build();
            participantRepository.save(participant);
            return 0;
        }
    }

    public List<TagDto> findTag() {
        List<Tag> tagList = tagRepository.findAll();
        List<TagDto> tagDtoList = tagList.stream()
                .map(tag -> TagDto.of(tag))
                .collect(Collectors.toList());

        return tagDtoList;
    }

    public List<MeetingInProgressResDto> findMeetingInProgress(Long userId) {
        List<MeetingInProgressResDto> meetingInProgressResDtoList = new ArrayList<>();

        // user가 참여 중인 Meeting List
        List<Meeting> meetingList = participantRepository.findMeetingByUserAndIsDone(userId, false);
        for (Meeting meeting : meetingList) {

            // 참여자 목록
            List<Participant> participantList = participantRepository.findByMeeting(meeting);
            List<ParticipantDto> participantDtoList = participantList.stream()
                    .map(participantDto -> ParticipantDto.of(participantDto))
                    .collect(Collectors.toList());

            // 참여자 수
            int participantCnt = participantList.size();

            // 모임에 해당하는 태그
            List<TagMeeting> tagMeetingList = tagMeetingRepository.findTagMeetingByMeeting
                    (meeting);
            List<TagDto> tagDtoList = tagMeetingList.stream()
                    .map(tagMeeting -> TagDto.of(tagMeeting.getTag()))
                    .collect(Collectors.toList());

            meetingInProgressResDtoList.add(MeetingInProgressResDto.builder()
                    .meeting(meeting)
                    .tag(tagDtoList)
                    .baseUrl(baseUrl)
                    .participant(participantDtoList)
                    .participantCnt(participantCnt)
                    .build());
        }

        return meetingInProgressResDtoList;
    }

    public List<MeetingCalenderResDto> findMeetingCalender(Long userId, Long meetingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        return answerRepository.findMeetingCalender(user, meeting);
    }


    public List<MeetingResultDto> findMeetingResultList(Long userId, Pageable pageable) {
        List<MeetingResultDto> meetingResultDtoList = new ArrayList<>();

        // user가 참여 완료 한 Meeting List
        List<Meeting> meetingList = participantRepository.findFinisihedMeetingByUser(
                userId,
                pageable);

        for (Meeting meeting : meetingList) {
            // 참여자 목록 가져오기
            List<Participant> participantList = participantRepository.findByMeeting(meeting);

            // List<Participant> -> List<ParticipantDto>
            List<ParticipantDto> participantDtoList = participantList.stream()
                    .map(participantDto -> ParticipantDto.of(participantDto))
                    .collect(Collectors.toList());

            // 참여자 수
            int participantCnt = participantList.size();

            // 모임에 해당하는 태그 가져오기
            List<TagMeeting> tagMeetingList = tagMeetingRepository.findTagMeetingByMeeting
                    (meeting);

            // List<TagMeeting> -> List<TagDto>
            List<TagDto> tagDtoList = tagMeetingList.stream()
                    .map(tagMeeting -> TagDto.of(tagMeeting.getTag()))
                    .collect(Collectors.toList());

            // 반환 List에 추가
            meetingResultDtoList.add(MeetingResultDto.builder()
                    .meeting(meeting)
                    .tag(tagDtoList)
                    .participant(participantDtoList)
                    .participantCnt(participantCnt)
                    .build());
        }
        return meetingResultDtoList;
    }

    public MeetingInviteUrlDto findMeetingInviteUrl(Long meetingId) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        return MeetingInviteUrlDto.builder()
                .meetingId(meeting.getId())
                .inviteUrl(baseUrl + meeting.getInviteCode())
                .build();
    }

    public MeetingChatDto findMeetingChat(Long userId, Long meetingId) {
        Meeting meeting = participantRepository.findMeetingByUserAndMeetingAndIsDone(userId, meetingId, true)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        // 참여자 수
        int participantCnt = participantRepository.findByMeeting(meeting).size();

        // 모임에 해당하는 태그 가져오기
        List<TagMeeting> tagMeetingList = tagMeetingRepository.findTagMeetingByMeeting(meeting);
        List<TagDto> tagDtoList = tagMeetingList.stream()
                .map(tagMeeting -> TagDto.of(tagMeeting.getTag()))
                .collect(Collectors.toList());

        // Question 목록
        List<Question> questionList = questionRepository.findByMeeting(meeting);
        List<ChatDto> chatDtoList = new ArrayList<>();
        for (Question question : questionList) {
            List<AnswerDto> answerDtoList = answerRepository.findMeetingChatResult(meeting, question, userId);
            ChatDto chatDto = ChatDto.builder()
                    .question(question)
                    .answer(answerDtoList)
                    .build();
            chatDtoList.add(chatDto);
        }

        return MeetingChatDto.builder()
                .meeting(meeting)
                .participantCnt(participantCnt)
                .tag(tagDtoList)
                .chat(chatDtoList)
                .build();
    }

    public List<MeetingAwardDto> findMeetingAward(Long userId, Long meetingId) {
        Meeting meeting = participantRepository.findMeetingByUserAndMeetingAndIsDone(userId, meetingId, true)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        // 해당 Meeting에 해당하는 모든 통계 가져오기
        List<MeetingAwardDto> meetingAwardDtoList = awardRepository.findAwardUser(meeting);

        // Type 별로 정렬
        Collections.sort(meetingAwardDtoList, new Comparator<MeetingAwardDto>() {
            @Override
            public int compare(MeetingAwardDto o1, MeetingAwardDto o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });

        return meetingAwardDtoList;
    }

    public List<FindUserResDto> findParticipant(Long userId, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        List<Participant> participantList = participantRepository.findByMeeting(meeting);

        Collections.sort(participantList, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                // 나를 최우선으로
                if (o2.getUser().getId() == userId) {
                    return 1;
                } else {
                    // 닉네임 사전순
                    return o1.getUser().getNickname().compareTo(o2.getUser().getNickname());
                }
            }
        });

        //  List<Participant> -> List<FindUserResDto>
        return participantList.stream().map(participant -> FindUserResDto.builder()
                .userId(participant.getUser().getId())
                .nickname(participant.getUser().getNickname())
                .profileImage(participant.getUser().getProfileImage())
                .build()).collect(Collectors.toList());
    }
}