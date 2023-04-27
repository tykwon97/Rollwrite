package com.rollwrite.domain.meeting.service;

import com.rollwrite.domain.meeting.dto.AddMeetingRequestDto;
import com.rollwrite.domain.meeting.dto.AddMeetingResponseDto;
import com.rollwrite.domain.meeting.dto.MeetingCalenderResDto;
import com.rollwrite.domain.meeting.dto.MeetingInProgressResDto;
import com.rollwrite.domain.meeting.dto.MeetingResultDto;
import com.rollwrite.domain.meeting.dto.ParticipantDto;
import com.rollwrite.domain.meeting.dto.TagDto;
import com.rollwrite.domain.meeting.entity.Meeting;
import com.rollwrite.domain.meeting.entity.Participant;
import com.rollwrite.domain.meeting.entity.Tag;
import com.rollwrite.domain.meeting.entity.TagMeeting;
import com.rollwrite.domain.meeting.repository.MeetingRepository;
import com.rollwrite.domain.meeting.repository.ParticipantRepository;
import com.rollwrite.domain.meeting.repository.TagMeetingRepository;
import com.rollwrite.domain.meeting.repository.TagRepository;
import com.rollwrite.domain.question.repository.AnswerRepository;
import com.rollwrite.domain.user.entity.User;
import com.rollwrite.domain.user.repository.UserRepository;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final MeetingRepository meetingRepository;
    private final AsyncMeetingService asyncMeetingService;
    private final TagMeetingRepository tagMeetingRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public AddMeetingResponseDto addMeeting(Long userId,
                                            AddMeetingRequestDto addMeetingRequestDto) throws NoSuchAlgorithmException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        log.info("1번");
        // 초대 코드 생성
        String inviteUrl = "http://localhost:8081/api/auth/join=";
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        log.info("2-1번");
        String inviteCode = Base64.getUrlEncoder().encodeToString(bytes);
        log.info("2-2번");


        // Meeting 생성
        Meeting meeting = Meeting.builder()
                .addMeetingRequestDto(addMeetingRequestDto)
                .inviteCode(inviteCode)
                .build();
        meetingRepository.save(meeting);

        log.info("3번");
        // tag id에 해당하는 Meeting(tagMeetingList)에 추가
        List<TagDto> tagList = new ArrayList<>();
        List<TagMeeting> tagMeetingList = tagIdToTagMeetingList(
                meeting, addMeetingRequestDto.getTag(), tagList);
        meeting.updateTagMeetingList(tagMeetingList);

        log.info("4번");
        // 질문에 사용 될 Tag
        String tag = "";
        for (TagDto tagDto : tagList) {
            tag += tagDto.getContent() + ",";
        }
        // Chat GPT 생성 질문 10개 저장
        asyncMeetingService.saveGptQuestion(tag, meeting);

        log.info("5번");
        // Meeting 생성자 Meeting에 추가
        Participant participant = Participant.builder()
                .user(user)
                .meeting(meeting)
                .build();
        participantRepository.save(participant);

        log.info("6번");
        return AddMeetingResponseDto.builder()
                .meeting(meeting)
                .tag(tagList)
                .inviteUrl(inviteUrl + inviteCode)
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
    public void joinMeeting(Long userId, Long meetingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다"));

        Participant participant = Participant.builder()
                .user(user)
                .meeting(meeting)
                .build();
        participantRepository.save(participant);
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


    public List<MeetingResultDto> findMeetingResult(Long userId, Pageable pageable) {
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
}
