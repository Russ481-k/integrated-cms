# 통합 CMS API 명세서

## 1. 메뉴 관리 API

### 1.1 메뉴 목록 조회
```http
GET /api/unified/services/{serviceId}/menus
```

**응답**
```json
{
  "success": true,
  "data": [
    {
      "menuId": 1,
      "menuName": "메인 메뉴",
      "menuCode": "MAIN",
      "menuType": "FOLDER",
      "displayPosition": "top",
      "sortOrder": 1,
      "visible": "ACTIVE",
      "children": [
        {
          "menuId": 2,
          "menuName": "서브 메뉴",
          "menuCode": "SUB",
          "menuType": "LINK",
          "url": "/sub",
          "displayPosition": "top",
          "sortOrder": 1,
          "visible": "ACTIVE",
          "children": []
        }
      ]
    }
  ]
}
```

### 1.2 메뉴 생성
```http
POST /api/unified/services/{serviceId}/menus

{
  "menuName": "string",
  "menuCode": "string",
  "menuType": "LINK" | "FOLDER" | "BOARD" | "CONTENT" | "PROGRAM",
  "url": "string",
  "targetId": number,
  "displayPosition": "string",
  "parentMenuId": number,
  "sortOrder": number,
  "visible": "ACTIVE" | "INACTIVE" | "HIDDEN",
  "requiredPermissions": string[]
}
```

### 1.3 메뉴 수정
```http
PUT /api/unified/services/{serviceId}/menus/{menuId}

{
  "menuName": "string",
  "menuType": "LINK" | "FOLDER" | "BOARD" | "CONTENT" | "PROGRAM",
  "url": "string",
  "targetId": number,
  "displayPosition": "string",
  "visible": "ACTIVE" | "INACTIVE" | "HIDDEN",
  "requiredPermissions": string[]
}
```

### 1.4 메뉴 순서/구조 변경
```http
PATCH /api/unified/services/{serviceId}/menus/{menuId}/position

{
  "parentMenuId": number,
  "sortOrder": number
}
```

### 1.5 메뉴 삭제
```http
DELETE /api/unified/services/{serviceId}/menus/{menuId}
```

## 2. 권한 관리 API

[이하 생략...]