# 통합 CMS API 명세서

## 📋 문서 정보

- **문서명**: 통합 CMS API 명세서
- **버전**: 2.0.0
- **최종 수정일**: 2024-03-25
- **상태**: 최신

---

## 1. API 아키텍처 개요

### 1.1 API 설계 원칙

#### 🎯 설계 철학

- **RESTful 설계**: HTTP 메서드와 상태 코드 활용
- **계층적 라우팅**: 통합 API와 서비스별 API 분리
- **보안 우선**: JWT 기반 인증과 세분화된 권한 관리
- **확장성**: 새로운 서비스 추가 시 최소 수정

#### 🌐 API 계층 구조

```
/api/
├── unified/           # 통합 관리 API
│   ├── auth/         # 인증/인가
│   ├── services/     # 서비스 관리
│   ├── content/      # 통합 콘텐츠 관리
│   ├── admins/       # 관리자 계정 관리
│   └── system/       # 시스템 관리
├── service1/         # 서비스1 전용 API
├── service2/         # 서비스2 전용 API
└── serviceN/         # 서비스N 전용 API
```

### 1.2 인증/인가 전략

#### 🔐 JWT 기반 인증

- **Access Token**: 15분 유효, API 호출용
- **Refresh Token**: 7일 유효, 토큰 갱신용
- **토큰 페이로드**: 사용자 ID, 권한, 서비스 접근 범위

#### 🛡️ 권한 검증 흐름

```mermaid
graph TD
    A[API 요청] --> B{토큰 유효성}
    B -->|유효| C{권한 확인}
    B -->|무효| D[401 Unauthorized]

    C --> E[통합 권한 조회]
    C --> F[서비스 권한 조회]
    C --> G[메뉴/기능 권한 조회]

    E --> H{권한 평가}
    F --> H
    G --> H

    H -->|허용| I[요청 처리]
    H -->|거부| J[403 Forbidden]

    I --> K[응답 반환]

    style A fill:#e1f5fe
    style I fill:#e8f5e8
    style D fill:#ffebee
    style J fill:#ffebee
```

---

## 2. 통합 관리 API

### 2.1 인증 및 권한 API

#### 🔑 인증 엔드포인트

**POST /api/unified/auth/login**

```json
// Request
{
  "username": "admin@example.com",
  "password": "secure_password",
  "rememberMe": false
}

// Response 200
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "admin@example.com",
      "fullName": "시스템 관리자",
      "permissions": ["SUPER_ADMIN"],
      "services": [1, 2, 3]
    }
  }
}

// Error Response 401
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "잘못된 인증 정보입니다.",
    "details": null
  }
}
```

**POST /api/unified/auth/refresh**

```json
// Request
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}

// Response 200
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 900
  }
}
```

**GET /api/unified/auth/me**

```json
// Response 200
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin@example.com",
    "fullName": "시스템 관리자",
    "email": "admin@example.com",
    "department": "시스템팀",
    "position": "팀장",
    "lastLoginAt": "2024-01-15T10:30:00Z",
    "permissions": {
      "unified": ["SERVICE_MANAGE", "ADMIN_MANAGE"],
      "services": {
        "service1": ["CONTENT_MANAGE", "BOARD_MANAGE"],
        "service2": ["CONTENT_READ", "POPUP_MANAGE"]
      }
    }
  }
}
```

### 2.2 서비스 관리 API

#### 🏢 서비스 등록 및 관리

### 2.3 메뉴 관리 API

#### 메뉴 시스템 개요

- **계층 구조**: 무제한 깊이의 트리 구조
- **서비스 독립성**: 서비스별 독립 메뉴 관리
- **통합 동기화**: 양방향 데이터 동기화
- **권한 관리**: RBAC + ABAC 기반 접근 제어

#### 메뉴 데이터 구조

```typescript
interface Menu {
  // 기본 정보
  id: number;
  name: string;
  type: 'LINK' | 'FOLDER' | 'BOARD' | 'CONTENT' | 'PROGRAM';
  url?: string;
  targetId?: number;
  displayPosition: string;
  sortOrder: number;
  parentId?: number;
  
  // 통합 관리
  serviceId?: number;
  menuCode?: string;
  menuLevel: number;
  visible: 'ACTIVE' | 'INACTIVE' | 'HIDDEN';
  requiredPermissions?: string[];
  config?: Record<string, any>;
  
  // 동기화
  unifiedMenuId?: number;
  unifiedSyncStatus: 'PENDING' | 'SUCCESS' | 'ERROR';
  unifiedLastSync?: Date;
}
```

#### API 엔드포인트

1. **메뉴 조회**
   ```http
   GET /api/unified/services/{serviceId}/menus
   ```

2. **메뉴 생성/수정**
   ```http
   POST /api/unified/services/{serviceId}/menus
   PUT /api/unified/services/{serviceId}/menus/{menuId}
   ```

3. **메뉴 구조 관리**
   ```http
   PATCH /api/unified/services/{serviceId}/menus/structure
   ```

4. **메뉴 동기화**
   ```http
   POST /api/unified/services/{serviceId}/menus/sync
   ```

#### 권한 체계

1. **기본 권한**
   - `MENU_READ`: 조회
   - `MENU_CREATE`: 생성
   - `MENU_UPDATE`: 수정
   - `MENU_DELETE`: 삭제
   - `MENU_MANAGE`: 전체 관리

2. **동적 권한**
   - `MENU_{CODE}_ACCESS`: 접근
   - `MENU_{CODE}_MANAGE`: 관리
```

**GET /api/unified/services**

```json
// Query Parameters
{
  "page": 1,
  "size": 20,
  "status": "ACTIVE",
  "search": "service"
}

// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "code": "service1",
        "name": "메인 서비스",
        "domain": "https://service1.example.com",
        "status": "ACTIVE",
        "lastHealthCheck": "2024-01-15T10:25:00Z",
        "healthStatus": "HEALTHY",
        "adminCount": 15,
        "contentCount": 1250
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 45,
      "totalPages": 3
    }
  }
}
```

**POST /api/unified/services**

```json
// Request
{
  "code": "new_service",
  "name": "새로운 서비스",
  "domain": "https://new.example.com",
  "apiBaseUrl": "https://api.new.example.com",
  "dbConnectionInfo": {
    "host": "db.new.example.com",
    "port": 3306,
    "database": "new_service_db",
    "username": "service_user",
    "password": "encrypted_password"
  },
  "settings": {
    "timezone": "Asia/Seoul",
    "language": "ko",
    "features": ["board", "content", "popup"]
  }
}

// Response 201
{
  "success": true,
  "data": {
    "id": 4,
    "code": "new_service",
    "name": "새로운 서비스",
    "status": "PENDING",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**POST /api/unified/services/{serviceId}/test**

```json
// Response 200 - 연결 성공
{
  "success": true,
  "data": {
    "status": "SUCCESS",
    "responseTime": 150,
    "details": {
      "database": "connected",
      "api": "accessible",
      "version": "1.2.3"
    }
  }
}

// Response 200 - 연결 실패
{
  "success": false,
  "error": {
    "code": "CONNECTION_FAILED",
    "message": "서비스 연결에 실패했습니다.",
    "details": {
      "database": "timeout",
      "api": "unreachable"
    }
  }
}
```

### 2.3 통합 콘텐츠 관리 API

#### 📝 콘텐츠 통합 조회

**GET /api/unified/content**

```json
// Query Parameters
{
  "page": 1,
  "size": 20,
  "serviceId": [1, 2],
  "contentType": "BOARD",
  "status": "PUBLISHED",
  "search": "공지사항",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "sortBy": "createdAt",
  "sortOrder": "desc"
}

// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1001,
        "serviceId": 1,
        "serviceName": "메인 서비스",
        "originalContentId": 123,
        "contentType": "BOARD",
        "title": "시스템 점검 안내",
        "author": "관리자",
        "status": "PUBLISHED",
        "syncStatus": "SUCCESS",
        "viewCount": 245,
        "createdAt": "2024-01-15T09:00:00Z",
        "updatedAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 1250,
      "totalPages": 63
    },
    "summary": {
      "totalByType": {
        "BOARD": 800,
        "CONTENT": 350,
        "POPUP": 100
      },
      "totalByStatus": {
        "PUBLISHED": 1000,
        "DRAFT": 200,
        "DELETED": 50
      }
    }
  }
}
```

**POST /api/unified/content/bulk-action**

```json
// Request
{
  "action": "UPDATE_STATUS",
  "contentIds": [1001, 1002, 1003],
  "parameters": {
    "status": "DRAFT",
    "reason": "일괄 임시저장 처리"
  }
}

// Response 200
{
  "success": true,
  "data": {
    "jobId": "bulk_001_20240115",
    "status": "PROCESSING",
    "total": 3,
    "processed": 0,
    "results": []
  }
}

// Job Status 조회: GET /api/unified/content/bulk-action/{jobId}
{
  "success": true,
  "data": {
    "jobId": "bulk_001_20240115",
    "status": "COMPLETED",
    "total": 3,
    "processed": 3,
    "results": [
      {
        "contentId": 1001,
        "success": true,
        "message": null
      },
      {
        "contentId": 1002,
        "success": false,
        "message": "권한이 없습니다."
      }
    ]
  }
}
```

### 2.4 관리자 계정 관리 API

#### 👥 관리자 CRUD

**GET /api/unified/admins**

```json
// Query Parameters
{
  "page": 1,
  "size": 20,
  "status": "ACTIVE",
  "department": "개발팀",
  "role": "SERVICE_ADMIN",
  "search": "홍길동"
}

// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "username": "hong@example.com",
        "fullName": "홍길동",
        "email": "hong@example.com",
        "department": "개발팀",
        "position": "선임",
        "status": "ACTIVE",
        "lastLoginAt": "2024-01-15T09:30:00Z",
        "groups": [
          {
            "id": 1,
            "name": "개발팀",
            "type": "DEPARTMENT"
          }
        ],
        "services": [
          {
            "id": 1,
            "name": "메인 서비스",
            "role": "CONTENT_ADMIN"
          }
        ],
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 85,
      "totalPages": 5
    }
  }
}
```

**POST /api/unified/admins**

```json
// Request
{
  "username": "newadmin@example.com",
  "password": "temp_password_123",
  "email": "newadmin@example.com",
  "fullName": "신규 관리자",
  "department": "운영팀",
  "position": "사원",
  "phone": "010-1234-5678",
  "groupIds": [2],
  "serviceRoles": [
    {
      "serviceId": 1,
      "roleId": 3
    }
  ],
  "menuPermissions": [
    {
      "menuId": 10,
      "permissionType": "ALLOW",
      "specificPermissions": {
        "board": ["read", "write"],
        "content": ["read"]
      }
    }
  ]
}

// Response 201
{
  "success": true,
  "data": {
    "id": 15,
    "username": "newadmin@example.com",
    "status": "PENDING_APPROVAL",
    "tempPassword": "temp_password_123",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

## 3. 서비스별 API

### 3.1 공통 패턴

#### 🔗 URL 패턴

```
/api/{serviceCode}/
├── auth/             # 서비스별 인증
├── board/            # 게시판 관리
├── content/          # 콘텐츠 관리
├── popups/           # 팝업 관리
├── menus/            # 메뉴 관리
├── files/            # 파일 관리
├── sync/             # 통합 관리 연동
└── settings/         # 서비스 설정
```

#### 🎭 권한 헤더

```http
GET /api/service1/board/articles
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Service-Code: service1
X-User-Permissions: BOARD_READ,CONTENT_READ
```

### 3.2 게시판 관리 API

#### 게시판 유형
- BASIC: 일반 게시판
- FAQ: 자주 묻는 질문
- QNA: 질문과 답변
- PRESS: 보도자료
- FORM: 신청 양식

#### 게시판 설정 API

**GET /api/{serviceCode}/board/master**

```json
// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "BBS_ID": 1,
        "BBS_NAME": "공지사항",
        "SKIN_TYPE": "BASIC",
        "READ_AUTH": "ROLE_USER",
        "WRITE_AUTH": "ROLE_ADMIN",
        "ADMIN_AUTH": "ROLE_ADMIN",
        "DISPLAY_YN": "Y",
        "SORT_ORDER": "D",
        "NOTICE_YN": "Y",
        "PUBLISH_YN": "Y",
        "ATTACHMENT_YN": "Y",
        "ATTACHMENT_LIMIT": 5,
        "ATTACHMENT_SIZE": 10
      }
    ]
  }
}
```

**POST /api/{serviceCode}/board/master**

```json
// Request
{
  "BBS_NAME": "공지사항",
  "SKIN_TYPE": "BASIC",
  "READ_AUTH": "ROLE_USER",
  "WRITE_AUTH": "ROLE_ADMIN",
  "ADMIN_AUTH": "ROLE_ADMIN",
  "DISPLAY_YN": "Y",
  "SORT_ORDER": "D",
  "NOTICE_YN": "Y",
  "PUBLISH_YN": "Y",
  "ATTACHMENT_YN": "Y",
  "ATTACHMENT_LIMIT": 5,
  "ATTACHMENT_SIZE": 10
}

// Response 201
{
  "success": true,
  "data": {
    "BBS_ID": 1
  }
}
```

#### 게시글 API

**GET /api/{serviceCode}/board/{bbsId}/articles**

```json
// Query Parameters
{
  "page": 1,
  "size": 20,
  "categoryId": 5,
  "noticeState": "Y",
  "publishState": "Y",
  "search": "공지",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31"
}

// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "NTT_ID": 123,
        "BBS_ID": 1,
        "MENU_ID": 10,
        "PARENT_NTT_ID": null,
        "THREAD_DEPTH": 0,
        "WRITER": "admin",
        "TITLE": "시스템 점검 안내",
        "content": "2024년 1월 20일 시스템 점검...",
        "NOTICE_STATE": "Y",
        "NOTICE_START_DT": "2024-01-15T00:00:00Z",
        "NOTICE_END_DT": "2024-01-16T00:00:00Z",
        "PUBLISH_STATE": "Y",
        "PUBLISH_START_DT": "2024-01-15T00:00:00Z",
        "PUBLISH_END_DT": "2024-01-16T00:00:00Z",
        "HITS": 245,
        "POSTED_AT": "2024-01-15T09:00:00Z",
        "DISPLAY_WRITER": "관리자",
        "has_image_in_content": true,
        "categories": [
          {
            "CATEGORY_ID": 5,
            "NAME": "일반공지"
          }
        ]
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 456,
      "totalPages": 23
    }
  }
}
```

**POST /api/{serviceCode}/board/{bbsId}/articles**

```json
// Request
{
  "TITLE": "새로운 공지사항",
  "content": "<p>공지 내용입니다.</p>",
  "NOTICE_STATE": "Y",
  "NOTICE_START_DT": "2024-01-15T00:00:00Z",
  "NOTICE_END_DT": "2024-01-16T00:00:00Z",
  "PUBLISH_STATE": "Y",
  "PUBLISH_START_DT": "2024-01-15T00:00:00Z",
  "PUBLISH_END_DT": "2024-01-16T00:00:00Z",
  "DISPLAY_WRITER": "관리자",
  "categories": [5]
}

// Response 201
{
  "success": true,
  "data": {
    "NTT_ID": 124
  }
}
```

#### 카테고리 API

**GET /api/{serviceCode}/board/{bbsId}/categories**

```json
// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "CATEGORY_ID": 5,
        "BBS_ID": 1,
        "CODE": "GENERAL",
        "NAME": "일반공지",
        "SORT_ORDER": 1,
        "DISPLAY_YN": "Y"
      }
    ]
  }
}
```

**POST /api/{serviceCode}/board/{bbsId}/categories**

```json
// Request
{
  "CODE": "GENERAL",
  "NAME": "일반공지",
  "SORT_ORDER": 1,
  "DISPLAY_YN": "Y"
}

// Response 201
{
  "success": true,
  "data": {
    "CATEGORY_ID": 5
  }
}
```

#### 댓글 API

**GET /api/{serviceCode}/board/articles/{nttId}/comments**

```json
// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "COMMENT_ID": 1,
        "NTT_ID": 123,
        "CONTENT": "댓글 내용입니다.",
        "WRITER": "user1",
        "DISPLAY_WRITER": "사용자1",
        "IS_DELETED": "N",
        "CREATED_AT": "2024-01-15T10:30:00Z"
      }
    ]
  }
}
```

**POST /api/{serviceCode}/board/articles/{nttId}/comments**

```json
// Request
{
  "CONTENT": "댓글 내용입니다.",
  "DISPLAY_WRITER": "사용자1"
}

// Response 201
{
  "success": true,
  "data": {
    "COMMENT_ID": 1
  }
}
```
```

### 3.3 콘텐츠 관리 API

**GET /api/{serviceCode}/content**

```json
// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 50,
        "type": "PAGE",
        "title": "회사 소개",
        "slug": "about-us",
        "status": "PUBLISHED",
        "template": "default",
        "author": {
          "id": 5,
          "name": "콘텐츠 관리자"
        },
        "publishedAt": "2024-01-10T00:00:00Z",
        "updatedAt": "2024-01-15T10:30:00Z",
        "metaData": {
          "description": "회사 소개 페이지입니다.",
          "keywords": ["회사", "소개", "about"],
          "ogImage": "https://example.com/og-about.jpg"
        }
      }
    ]
  }
}
```

### 3.4 통합 관리 연동 API

**GET /api/{serviceCode}/sync/status**

```json
// Response 200
{
  "success": true,
  "data": {
    "serviceCode": "service1",
    "lastSyncAt": "2024-01-15T10:25:00Z",
    "syncStatus": "SUCCESS",
    "pendingItems": 3,
    "errorItems": 0,
    "details": {
      "board": {
        "total": 456,
        "synced": 453,
        "pending": 2,
        "error": 1
      },
      "content": {
        "total": 89,
        "synced": 89,
        "pending": 0,
        "error": 0
      },
      "popup": {
        "total": 15,
        "synced": 14,
        "pending": 1,
        "error": 0
      }
    }
  }
}
```

**POST /api/{serviceCode}/sync/request**

```json
// Request
{
  "type": "FULL",
  "targets": ["board", "content", "popup"],
  "force": false
}

// Response 202
{
  "success": true,
  "data": {
    "jobId": "sync_service1_20240115_001",
    "status": "QUEUED",
    "estimatedTime": 300
  }
}
```

---

## 4. 에러 처리 및 응답 형식

### 4.1 표준 응답 형식

#### ✅ 성공 응답

```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_123456789",
    "version": "1.0.0"
  }
}
```

#### ❌ 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값이 올바르지 않습니다.",
    "details": {
      "field": "email",
      "value": "invalid-email",
      "constraint": "이메일 형식이 아닙니다."
    }
  },
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_123456789"
  }
}
```

### 4.2 HTTP 상태 코드

| 코드 | 용도                  | 설명             |
| ---- | --------------------- | ---------------- |
| 200  | OK                    | 요청 성공        |
| 201  | Created               | 리소스 생성 성공 |
| 202  | Accepted              | 비동기 작업 접수 |
| 400  | Bad Request           | 잘못된 요청      |
| 401  | Unauthorized          | 인증 실패        |
| 403  | Forbidden             | 권한 없음        |
| 404  | Not Found             | 리소스 없음      |
| 409  | Conflict              | 리소스 충돌      |
| 422  | Unprocessable Entity  | 검증 실패        |
| 429  | Too Many Requests     | 요청 한도 초과   |
| 500  | Internal Server Error | 서버 오류        |

### 4.3 에러 코드 체계

#### 🔍 에러 코드 패턴

```
{CATEGORY}_{TYPE}_{DETAIL}

예시:
- AUTH_TOKEN_EXPIRED: 토큰 만료
- VALIDATION_FIELD_REQUIRED: 필수 필드 누락
- PERMISSION_ACCESS_DENIED: 접근 권한 없음
- SERVICE_CONNECTION_FAILED: 서비스 연결 실패
```

#### 📋 주요 에러 코드

```json
{
  // 인증/인가 에러
  "AUTH_TOKEN_INVALID": "유효하지 않은 토큰입니다.",
  "AUTH_TOKEN_EXPIRED": "토큰이 만료되었습니다.",
  "AUTH_CREDENTIALS_INVALID": "잘못된 인증 정보입니다.",
  "AUTH_ACCOUNT_LOCKED": "계정이 잠겼습니다.",

  // 권한 에러
  "PERMISSION_ACCESS_DENIED": "접근 권한이 없습니다.",
  "PERMISSION_INSUFFICIENT": "권한이 부족합니다.",
  "PERMISSION_SERVICE_RESTRICTED": "서비스 접근이 제한되었습니다.",

  // 검증 에러
  "VALIDATION_FIELD_REQUIRED": "필수 필드가 누락되었습니다.",
  "VALIDATION_FIELD_INVALID": "필드 값이 올바르지 않습니다.",
  "VALIDATION_DUPLICATE_VALUE": "중복된 값입니다.",

  // 서비스 에러
  "SERVICE_UNAVAILABLE": "서비스를 사용할 수 없습니다.",
  "SERVICE_CONNECTION_FAILED": "서비스 연결에 실패했습니다.",
  "SERVICE_TIMEOUT": "서비스 응답 시간이 초과되었습니다.",

  // 리소스 에러
  "RESOURCE_NOT_FOUND": "리소스를 찾을 수 없습니다.",
  "RESOURCE_CONFLICT": "리소스 충돌이 발생했습니다.",
  "RESOURCE_LOCKED": "리소스가 잠겼습니다."
}
```

---

## 5. API 보안

### 5.1 요청 보안

#### 🛡️ 헤더 보안

```http
# 필수 보안 헤더
Authorization: Bearer {jwt_token}
X-Requested-With: XMLHttpRequest
X-CSRF-Token: {csrf_token}
Content-Type: application/json

# 선택적 헤더
X-Client-Version: 1.2.3
X-Client-Platform: web
X-Forwarded-For: 192.168.1.100
```

#### 🔒 CORS 설정

```json
{
  "allowedOrigins": [
    "https://admin.example.com",
    "https://unified.example.com"
  ],
  "allowedMethods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "allowedHeaders": [
    "Authorization",
    "Content-Type",
    "X-Requested-With",
    "X-CSRF-Token"
  ],
  "exposedHeaders": ["X-Total-Count", "X-Page-Count"],
  "allowCredentials": true,
  "maxAge": 3600
}
```

### 5.2 Rate Limiting

#### 📊 제한 정책

```json
{
  "global": {
    "requests": 1000,
    "window": "1h",
    "burst": 100
  },
  "authenticated": {
    "requests": 5000,
    "window": "1h",
    "burst": 200
  },
  "admin": {
    "requests": 10000,
    "window": "1h",
    "burst": 500
  },
  "bulk_operations": {
    "requests": 10,
    "window": "1m",
    "burst": 3
  }
}
```

#### 🚦 Rate Limit 응답

```json
// 429 Too Many Requests
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "요청 한도를 초과했습니다.",
    "details": {
      "limit": 1000,
      "remaining": 0,
      "resetAt": "2024-01-15T11:00:00Z"
    }
  }
}

// Response Headers
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705320000
Retry-After: 3600
```

---

## 6. API 문서 및 테스팅

### 6.1 OpenAPI 스펙

#### 📖 Swagger 설정

```yaml
openapi: 3.0.3
info:
  title: 통합 CMS API
  description: 다중 서비스 통합 관리 CMS API
  version: 1.0.0
  contact:
    name: 개발팀
    email: dev@example.com

servers:
  - url: https://api.unified-cms.com/api
    description: Production API
  - url: https://staging-api.unified-cms.com/api
    description: Staging API

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - bearerAuth: []

paths:
  /unified/auth/login:
    post:
      tags: [Authentication]
      summary: 관리자 로그인
      description: 통합 관리자 인증을 수행합니다.
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/LoginRequest"
      responses:
        "200":
          description: 로그인 성공
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/LoginResponse"
        "401":
          description: 인증 실패
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
```

### 6.2 API 테스팅 전략

#### 🧪 테스트 레벨

1. **단위 테스트**: 개별 엔드포인트 로직 테스트
2. **통합 테스트**: 서비스 간 연동 테스트
3. **E2E 테스트**: 전체 워크플로우 테스트
4. **부하 테스트**: 성능 및 안정성 테스트

#### 📋 테스트 케이스 예시

```javascript
// Jest + Supertest 예시
describe("통합 관리 API", () => {
  describe("POST /api/unified/auth/login", () => {
    it("유효한 인증정보로 로그인 성공", async () => {
      const response = await request(app)
        .post("/api/unified/auth/login")
        .send({
          username: "admin@example.com",
          password: "valid_password",
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.accessToken).toBeDefined();
      expect(response.body.data.user.username).toBe("admin@example.com");
    });

    it("잘못된 비밀번호로 로그인 실패", async () => {
      const response = await request(app)
        .post("/api/unified/auth/login")
        .send({
          username: "admin@example.com",
          password: "wrong_password",
        })
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.error.code).toBe("INVALID_CREDENTIALS");
    });
  });
});
```

---

## 7. 성능 최적화

### 7.1 응답 최적화

#### 🚀 데이터 압축

```javascript
// gzip 압축 설정
app.use(
  compression({
    filter: (req, res) => {
      if (req.headers["x-no-compression"]) {
        return false;
      }
      return compression.filter(req, res);
    },
    threshold: 1024,
  })
);
```

#### 📊 페이지네이션

```json
// 커서 기반 페이지네이션
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "hasNext": true,
      "hasPrev": true,
      "nextCursor": "eyJpZCI6MTAwfQ==",
      "prevCursor": "eyJpZCI6ODB9",
      "total": 1500
    }
  }
}
```

### 7.2 캐싱 전략

#### 🗄️ Redis 캐싱

```javascript
// 권한 정보 캐싱
const getCachedPermissions = async (userId) => {
  const cacheKey = `permissions:${userId}`;
  let permissions = await redis.get(cacheKey);

  if (!permissions) {
    permissions = await permissionService.getUserPermissions(userId);
    await redis.setex(cacheKey, 300, JSON.stringify(permissions)); // 5분 캐시
  }

  return JSON.parse(permissions);
};

// 서비스 목록 캐싱
const getCachedServices = async () => {
  const cacheKey = "services:active";
  let services = await redis.get(cacheKey);

  if (!services) {
    services = await serviceRepository.findActiveServices();
    await redis.setex(cacheKey, 600, JSON.stringify(services)); // 10분 캐시
  }

  return JSON.parse(services);
};
```

---

## 8. 관련 문서

### 📚 연관 가이드

- **[시스템 개요](./unified-cms-system-overview.md)** - 전체 시스템 아키텍처
- **[데이터베이스 스키마](./unified-cms-database-schema.md)** - 데이터 구조 설계
- **[프론트엔드 가이드](./unified-cms-frontend-guide.md)** - UI/UX 설계
- **[배포 운영 가이드](./unified-cms-deployment-guide.md)** - 운영 및 배포
- **[고급 권한 시스템](./advanced-permission-system-guide.md)** - 권한 관리 상세

### 🔗 외부 참조

- [JWT.io](https://jwt.io/) - JWT 토큰 디버깅
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [REST API Design Guidelines](https://restfulapi.net/)
- [HTTP Status Code Reference](https://httpstatuses.com/)

---

이 API 가이드를 통해 통합 CMS의 모든 API 엔드포인트를 체계적으로 설계하고 구현할 수 있습니다. 보안, 성능, 확장성을 모두 고려한 REST API 설계로 안정적이고 효율적인 서비스를 제공할 수 있습니다.
