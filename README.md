# 개인 가계부 관리 프로그램

자바프로그래밍 기말 대체 프로젝트로 제작한 Java Swing 기반 개인 가계부 프로그램입니다.
수입/지출 내역을 입력하고, 월별 총수입/총지출/잔액과 카테고리별 지출 합계를 확인할 수 있습니다.
프로그램 종료 후에도 CSV 파일로 데이터를 저장하고 다시 불러올 수 있습니다.

## 주요 기능

- 수입 및 지출 내역 추가
- 전체 내역 JTable 조회
- 선택한 내역 수정 및 삭제
- 월별 총수입, 총지출, 잔액 계산
- 카테고리별 지출 합계 표시
- CSV 파일 저장 및 불러오기
- 날짜/금액/카테고리 입력 검증

## 실행 방법

```bash
javac -encoding UTF-8 src/*.java
java -cp src Main
```

실행 후 `data/transactions.csv` 파일이 자동 저장됩니다.
처음 실행하거나 파일이 없으면 샘플 데이터가 자동으로 생성됩니다.

## 파일 구성

```text
src/
  Main.java          # GUI 실행 및 이벤트 처리
  BudgetManager.java # 목록 관리, 계산, 파일 입출력
  Transaction.java   # 수입/지출 데이터 모델
report/
  final_report.pdf   # 제출용 최종 보고서
assets/
  screenshot_*.png   # 보고서 삽입용 화면 이미지
```

## GitHub 업로드

제출 전 본인 GitHub에 저장소를 만든 뒤 `src` 폴더와 README를 업로드하면 됩니다.
예시 저장소명: `java-budget-manager`
