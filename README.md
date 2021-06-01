<img src="https://user-images.githubusercontent.com/43202607/120275817-d49c1000-c2ec-11eb-87b5-5f7a1d8c8d1b.png" width="200" height="100">

## 소개
드로잉투게더 모바일 앱의 사용 현황을 모니터링하는 자바 애플리케이션입니다.  
  
MQTT 브로커 로그 내용과 모바일 앱에서 마스터가 주기적으로 송신하는 회의방 정보를 수집합니다.   
이를통해 드로잉투게더의 **실시간 사용 현황**과 **누적 사용 현황**을 확인합니다.

<br>
<p align="center">
  <img src="https://user-images.githubusercontent.com/43202607/120277372-f1d1de00-c2ee-11eb-9373-a74c3439ebc1.png" width="800" height="500">
</p>  
  
<br>

## 기능    
* **MQTT 브로커 로그 파싱**   
MQTT 브로커가 출력하는 로그내용을 읽어, 전체 참여자 및 각각의 회의방에서 주고받는 메시지 양과 메시지 전송 횟수, 참여자 수를 수집합니다.
  
* **회의방 정보 메시지 수신**  
회의방의 마스터가 전송하는 메시지를 받아 회의방의 정보를 수집합니다.  
  
* **데이터베이스 업로드**  
수집된 정보들을 데이터베이스에 저장합니다.  
  
<br>

## 모니터링 데이터
**전체 회의방의 메시지 트래픽 정보**와 **개별 회의방의 정보**를 수집합니다. 모니터링하는 데이터는 총 6개이며 종류는 아래와 같습니다.  
  
#### MQTT 브로커에서 처리되는 메시지 트래픽 정보  
  
* 사용중인 회의방 메시지 처리량/송신 횟수 및 참가자 수  
* 사용이 종료된 회의방 메시지 누적 처리량/송신 횟수 및 누적 참가자 수  
* 참가자 메시지 처리량/송신 횟수  
* 참가자 모바일 운영체제  
  
#### 드로잉투게더 회의방 화면 정보  
  
* 사용중인 회의방의 자유곡선, 도형, 텍스트, 이미지, 지우개 사용 횟수  
* 사용이 종료된 회의방의 자유곡선, 도형, 텍스트, 이미지, 지우개 사용 횟수  
  
<br>

## 모니터링 데이터 수집 방법
### MQTT 브로커 로그 파싱
브로커에서 출력하는 로그를 바탕으로 **MQTT 브로커에서 처리되는 메시지 트래픽 정보**를 수집합니다.  
로그를 통해 참가자들의 접속과 퇴장, 참가자들이 주고받는 메시지의 크기와 메시지를 전송하는 횟수를 알 수 있습니다.  
  
사용자가 입력한 회의명과 이름을 바탕으로 드로잉투게더 시스템에서 MQTT 클라이언트 아이디를 생성합니다. 이때 아이디를 “* 참가자 이름_ 토픽_ 모바일 운영체제” 형식으로 지정하여 브로커에 연결합니다. 아이디 형식에서 ‘*’ 문자는 주요 데이터를 전송하는 클라이언트임을 나타내기 위해 표시합니다. 또한 아이디가 중복되지 않도록 하기 위하여 참여자의 이름 뿐만 아니라 회의명도 포함시킵니다. 그리고 참가자가 사용하는 모바일 운영체제를 표시합니다.  
  
<p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120287254-0ff10b80-c2fa-11eb-8d3f-fde50276f39e.png"><br>
	<em>Android 참가자의 클라이언트 아이디</em>
</p>

<p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120287425-344ce800-c2fa-11eb-8371-2ebaf2eb379b.png"><br>
	<em>iOS 참가자의 MQTT 클라이언트 아이디</em>
</p>  

<br>

모니터링 애플리케이션은 브로커 로그를 읽어 수집된 정보를 일정 초 간격으로 데이터베이스에 업데이트합니다. 브로커 로그에서 특정 키워드를 읽어 들여 생성된 회의 정보 및 사용중인 참가자 정보를 저장합니다. 그리고 사용이 종료된 회의 및 퇴장한 참가자를 관리합니다. 로그에서 추출하는 키워드는 아래 그림들과 같습니다.  
  
참가자가 회의방에 참가하면 입력한 회의명에서 파생된 8개의 토픽을 구독하는데 그중 처음으로 구독하는 join 토픽을 바탕으로 회의방에 대한 정보를 수집합니다.  
  
<p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120288320-09af5f00-c2fb-11eb-9fa9-ed426003d19d.png"><br>
	<em>회의방 정보를 수집하는 키워드</em>
</p>
  
 <br>
  
참가자가 접속하면 참가자 이름과 회의명, 운영체제를 바탕으로 생성된 아이디로 MQTT 클라이언트를 생성하여 브로커에 연결하는 과정을 거치게 되는데 이때 출력되는 사용중인 참가자의 정보를 수집합니다. “*참가자 이름_회의명_모바일 운영체제” 형식의 MQTT 클라이언트 아이디에서 해당 이름을 가진 참가자가 어떤 회의방에 참여중인지와 참가자의 모바일 운영체제를 확인할 수 있습니다.  

<p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120288320-09af5f00-c2fb-11eb-9fa9-ed426003d19d.png"><br>
	<em>사용중인 참가자 정보를 수집하는 키워드</em>
</p>

<br>

마스터가 close 토픽으로 메시지를 보내는 경우 회의방에 종료됨을 의미하므로 브로커가 close 토픽으로 메시지를 받은 경우 회의방이 종료되었음을 확인할 수 있습니다. 회의방이 종료되면 기존 회의에 대해 실시간으로 수집하던 데이터를 데이터베이스에서 지우고 회의명을 “회의명(종료시간)”으로 변경하여 회의 진행동안 전송한 누적 메시지 크기 및 누적 메시지 전송 횟수, 누적 참가자 수를 구하여 데이터베이스에 저장합니다.    
  
 <p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120291973-d40c7500-c2fe-11eb-8326-2eb5b3df9bc5.png"><br>
	<em>사용이 종료된 회의 정보를 수집하는 키워드</em>
</p> 
 
 <br>
 
앞서 설명한 회의방 생성 및 사용자 참가 관리 외에 회의 참여 중에 참가자들이 보낸 메시지들에 대한 로그들을 읽어 메시지를 전송한 횟수와 전송한 메시지 크기 정보를 수집합니다. 이때 드로잉 메시지와 이미지 메시지를 전송하는 토픽에 대한 로그만 읽습니다. 로그에서 수집하는 단어는 아래 그림과 같습니다.  
  
 <p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120292188-0c13b800-c2ff-11eb-85e6-40ebedb2496d.png"><br>
	<em>메시지 트래픽 정보를 수집하는 키워드</em>
</p>

<br>  

### 회의방 정보 메시지 수신
참가자들이 전송하는 메시지들의 로그를 바탕으로 **전체 회의방의 정보**와 **개별 회의방의 정보**를 수집할 수 있습니다.  
  
모니터링 애플리케이션에서는 모니터링을 위한 토픽을 구독하고 브로커를 통해 드로잉투게더에서 주기적으로 전송하는 회의방 데이터를 전달받습니다. 드로잉투게더에서는 회의명과 해당 회의방에서 현재까지 그려진 자유 곡선, 사각형, 타원, 텍스트 개수와 이미지 전송 횟수, 지우개 사용 횟수에 대한 메시지를 마스터가 모니터링 토픽으로 송신합니다. 회의방 정보 수신 과정은 아래 그림과 같습니다.  
  
<p align="center">
	<img src="https://user-images.githubusercontent.com/43202607/120292712-7fb5c500-c2ff-11eb-87f8-79be26cdf3c5.png"> <br>
	<em>회의방 정보 메시지 송수신 과정</em>
</p>

<br>
  
  
## 데이터베이스 구조  
모니터링한 데이터는 주기적으로 데이터베이스에 저장되며 테이블의 구조는 아래 표와 같습니다.    
  
#### 전체 테이블
| 테이블 | 설명 |
|---|---|
| realtime | 전체 회의방의 참가자 수, 메시지 사이즈, 메시지 전송 횟수, 메시지 송신자 수를 저장하는 테이블 |
| topic | 회의방이 시작된 시간과 참여자 수, 회의에서 전달하는 메시지 사이즈, 메시지 전송 횟수를 저장하는 테이블 |
| client | 각 참여자들의 메시지 전송 횟수, 메시지 사이즈, 플랫폼, 참여중인 회의명을 저장하는 테이블 |
| component | 회의방에 현재까지 그려진 자유 곡선, 사각형, 타원, 텍스트 개수와 이미지 전송 횟수, 지우개 사용 횟수를 저장하는 테이블 |
  
#### realtime 테이블

| date(문자열) | number\_of\_ connections (개수) | accumulated\_msg\_size (크기) | msg\_publish\_count (개수) | number\_of\_senders (개수) |
|---|---|---|---|---|
| 2020-08-31 15:08:11 | 5 | 2632 | 23 | 3 |
| 2020-08-31 15:08:14 | 5 | 574 | 12 | 2 |
| 2020-08-31 15:08:17 | 5 | 1443 | 51 | 4 |

#### topic 테이블

| topic (문자열) | accumulated\_msg\_size  (크기) | start\_date  (문자열) | participants  (개수) |
|---|---|---|---|
| meeting1 | 2408 | 2020-08-31 15:01:25 | 2 |
| meeting2 | 574 | 2020-08-31 15:06:59 | 2 |
| meeting3 | 1443 | 2020-08-31 15:07:53 | 1 |
| meeting4(2020-08-31 15:08:11) | 54623 | 2020-08-31 14:58:22 | 3 |

#### client 테이블

| topic (문자열) | accumulated\_msg\_size  (크기) | start\_date  (문자열) | participants  (개수) |
|---|---|---|---|
| meeting1 | 2408 | 2020-08-31 15:01:25 | 2 |
| meeting2 | 574 | 2020-08-31 15:06:59 | 2 |
| meeting3 | 1443 | 2020-08-31 15:07:53 | 1 |
| meeting4(2020-08-31 15:08:11) | 54623 | 2020-08-31 14:58:22 | 3 |

#### component 테이블

| topic (문자열) | stroke  (개수) | rect  (개수) | oval  (개수) | text  (개수) | image  (개수) | erase  (개수) |
|---|---|---|---|---|---|---|
| meeting1 | 34 | 12 | 6 | 10 | 2 | 23 |
| meeting2 | 52 | 9 | 13 | 6 | 1 | 40 |
| meeting3 | 3 | 2 | 2 | 5 | 4 | 5 |
| meeting4(2020-08-31 15:08:11) | 32 | 10 | 4 | 3 | 2 | 34 |

<br>

## 사용된 라이브러리
| 컴포넌트 | 버전 | 라이선스 |
|---|---|---|
| mysql-connector-java | 5.1.46 | GPL 2.0 |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | EPL 2.0 |
| gson | 2.8.6 | Apache 2.0 |

