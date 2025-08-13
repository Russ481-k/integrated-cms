import {
  LuUser,
  LuHouse,
  LuLock,
  LuAppWindow,
  LuFileText,
} from "react-icons/lu";
export const MenuItems = [
  { icon: LuHouse, label: "HOME", path: "/home" },
  { icon: LuFileText, label: "메뉴관리", path: "/menu" },
  { icon: LuAppWindow, label: "서비스관리", path: "/service" },
  { icon: LuUser, label: "관리자관리", path: "/admin" },
  { icon: LuLock, label: "권한관리", path: "/role" },

  // { icon: LuActivity, label: "Program_1", path: "/program_1" },
  // { icon: LuActivity, label: "Program_2", path: "/program_2" },
  // { icon: LuActivity, label: "Program_3", path: "/program_3" },
  // { icon: LuActivity, label: "Program_4", path: "/program_4" },

  // { icon: LuLayoutPanelLeft, label: "컨텐츠관리", path: "/content" },
  // { icon: LuCalendar, label: "게시판관리", path: "/board" },
  // { icon: LuFiles, label: "파일관리", path: "/file" },
  // { icon: LuLayoutPanelTop, label: "템플릿관리", path: "/template" },
  // { icon: LuAlarmClockCheck, label: "일정관리", path: "/schedule" },
  // { icon: LuFiles, label: "수영장관리", path: "/swimming" },
  // { icon: LuMessageCircle, label: "문의관리", path: "/inquiry" },
  // { icon: LuLayers3, label: "팝업관리", path: "/popup" },
];
