import styled from "styled-components";

const InfoContainer = styled.div`
  width: 100%;
  height: 8vh;
`;

const DateContainer = styled.div`
  width: 100%;
  font-size: 18px;
  color: var(--darkgray-color);
  text-align: center;
  margin-block: 5px;
`;

const NameContainer = styled.div`
  width: 100%;
  font-size: 20px;
  color: var(--black-color);
  text-align: center;
  margin-block: 5px;
`;

const EmojiContainer = styled.div`
  width: 100%;
  margin-block: 30px;
  display: flex;
  align-items: space-between;
`;

const ArrowContainer = styled.div`
  margin: auto 15px;
`;

const TextContainer = styled.div`
  width: 100%;
  height: 8vh;
  font-size: 20px;
  color: var(--black-color);
  text-align: center;
  font-weight: 700;
  line-height: 24px;
  padding: 0 15px;
`;

const BtnContainer = styled.div`
  width: 100%;
  margin-block: 50px;
`;

export {
    InfoContainer, 
    DateContainer, 
    NameContainer, 
    EmojiContainer,
    ArrowContainer, 
    TextContainer, 
    BtnContainer 
};
