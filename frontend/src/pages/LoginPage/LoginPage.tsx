import React from "react";
import { LogoContainer, BtnContainer } from "./style";
import { ReactComponent as Logo } from "../../assets/Logo.svg";
import { ReactComponent as KakaoBtn } from "../../assets/Kakao.svg";

import { redirectKakao } from "../../apis/user";
import { persistor } from "../../store/store";

function LoginPage() {
  const purge = async () => {
    await persistor.purge();
  };

  const handleClickLoginBtn = () => {
    purge();
    redirectKakao();
  };

  return (
    <>
      <LogoContainer>
        <Logo />
        <p>
          친구들과 특별한 추억을 <br /> 만들어 볼까요?
        </p>
      </LogoContainer>
      <BtnContainer>
        <KakaoBtn onClick={handleClickLoginBtn} />
      </BtnContainer>
    </>
  );
}

export default LoginPage;
