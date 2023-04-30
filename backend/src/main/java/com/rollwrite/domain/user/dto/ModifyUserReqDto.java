package com.rollwrite.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString
public class ModifyUserReqDto {
    private String nickname;
    private String profileImage;

}
