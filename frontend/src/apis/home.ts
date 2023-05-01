import { axiosInstance } from "./instance";
import { CreateGroup } from "../constants/types";

// 모임 만들기
export const createGroup = async (groupInfo: CreateGroup) => {
  const response = await axiosInstance.post("/meeting", groupInfo);
  return response.data;
};

// 모임 가져오기
export const getGroupList = async () => {
  const response = await axiosInstance.get("/meeting");
  return response.data;
};

// 내가 응답한 질문 리스트 가져오기
export const getQuestionList = async (id: number) => {
  const response = await axiosInstance.get(`/meeting/${id}`);
  return response.data;
};

// 그룹태그 가져오기
export const getGroupTag = async () => {
  const response = await axiosInstance.get(`/meeting/tag`);
  return response.data;
};

// 질문 생성하기
export const createQuestion = async (id: number, question: string) => {
  const response = await axiosInstance.post(`/question`, {
    meetingId: id,
    question: question,
  });
  return response.data;
};

// 초대 링크 가져오기
export const getInviteUrl = async (meetingId: string) => {
  const response = await axiosInstance.get(`/meeting/join/${meetingId}`);
  return response.data;
};

// 모임 가입 하기
export const joinGroup = async (inviteCode: string) => {
  const response = await axiosInstance.post(`/meeting/join/${inviteCode}/2`);
  return response.data;
};

// 상장수여
export const getAwardMember = async (meetingId: string) => {
  const response = await axiosInstance.get(`meeting/award/${meetingId}`);
  return response.data;
};

// 로그인
// 카카오 로그인 리다이렉트
export const redirectKakao = async () => {
  const CLIENT_ID = "88cb08e0de73021429ec359e909db650";
  // const REDIRECT_URI = "http://localhost:3000/login/oauth";
  const REDIRECT_URI = `https://k8a508.p.ssafy.io/api/auth/kakao/login`;
  const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;
  const response = await axiosInstance.get(KAKAO_AUTH_URL);
  return response.data;
};
