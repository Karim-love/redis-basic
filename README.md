# 📃 Redis 기본 코드들
```
>> redis gradle version
lettuce-core : 6.1.5.RELEASE

>> 구축 redis version
Redis : 6.0.7
```

## ❓ Redis 동작에 대한 기본 기능
redis 기본 동작에 대해 pkg 별로 구현한 프로젝트입니다.

## ✔️Pakage 별 정리

## 1. utils, logger, cfg
- 기본 기능을 제공하는 부속 기능 및 유틸 모음

## 2. define
- 공통 정의 변수 모음

## 3. instance / object 
- redis 접속 정보를 정의해 놓은 instance
- lettuce api 의 기본 기능을 구현해 놓은 object
  - 접속 / 핑 / 종료 등 구현
    > http://redisgate.kr/redis/clients/lettuce_intro.php  
    > single / cluster

## 4. redis_type
- 데이터 타입별로 명령어 기능 구현
   > sync / async

