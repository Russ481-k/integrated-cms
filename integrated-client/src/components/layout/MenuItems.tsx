import {
  LuFileText,
  LuCalendar,
  LuUser,
  LuFiles,
  LuLayoutPanelLeft,
  LuHouse,
  LuLock,
} from "react-icons/lu";
export const MenuItems = [
  { icon: LuHouse, label: "HOME", path: "/home" },
  { icon: LuFileText, label: "메뉴관리", path: "/menu" },
  { icon: LuLayoutPanelLeft, label: "컨텐츠관리", path: "/content" },
  { icon: LuCalendar, label: "게시판관리", path: "/board" },
  { icon: LuFiles, label: "파일관리", path: "/file" },
  { icon: LuUser, label: "관리자관리", path: "/admin" },
  { icon: LuLock, label: "권한관리", path: "/role" },
  // { icon: LuLayoutPanelTop, label: "템플릿관리", path: "/template" },
  // { icon: LuAlarmClockCheck, label: "일정관리", path: "/schedule" },
  // { icon: LuFiles, label: "수영장관리", path: "/swimming" },
  // { icon: LuMessageCircle, label: "문의관리", path: "/inquiry" },
  // { icon: LuLayers3, label: "팝업관리", path: "/popup" },
];
