import { useDispatch, useSelector } from "react-redux";
import type { TypedUseSelectorHook } from "react-redux";
import type { RootState, AppDispatch } from "../store/store";
import { ReactElement } from "react";

export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export const screenHight = window.innerHeight;

export interface CreateGroup {
  title: string;
  tag: number[];
  startDay: string;
  endDay: string;
  color: string;
}

export interface Tag {
  tagId: number;
  content: string;
}

export interface Participant {
  userId: number;
  nickname: string;
  profileImage: string;
  type?: string;
}

export interface GroupInfo {
  meetingId: number;
  title: string;
  tag: Tag[];
  startDay: string;
  endDay: string;
  color: string;
  participant: Participant[];
  participantCnt: number;
  questionLimit: number;
  questionUsage: number;
  inviteUrl?: string;
}

export interface Question {
  day: string;
  question: string;
}

export interface DayInfo {
  currentDay: Date;
  formattedDate: string;
  isToday: boolean;
  isCurrMonth: boolean;
  sprout: number;
  question: string;
}

export interface Profile {
  nickname: string;
  profileImage: string;
  cntMeetingProgress?: number;
  cntMeetingProgressIsDone?: number;
}

export interface QuestionInfo {
  meetingId: number;
  title: string;
  day: number;
  questionId: number;
  question: string;
  emoji: string;
  answer: string;
  image: string;
  isFinal: boolean;
}

export interface AnswerInfo {
  answer: string;
  meetingId: number;
  questionId: number;
  image?: string;
}

export interface Chat {
  questionId: number;
  question: string;
  day: string;
  answer?: {
    nickname: string;
    imageUrl: string;
    profileImage: string;
    isMe: boolean;
    content: string;
    time: string;
  }[];
}

export interface Award {
  taleteller: Participant[];
  photographer: Participant[];
  perfectAttendance: Participant[];
}

export interface GroupResult {
  meetingId: number;
  title: string;
  tag: Tag[];
  startDay: string;
  endDay: string;
  color: string;
  participantCnt: number;
  chat: Chat[];
}

export interface Notify {
  id: number;
  type: number;
  content: string;
}

export interface AccordionItemType {
  title: string;
  content: ReactElement;
}