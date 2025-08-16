#!/bin/bash

# 통합 CMS v2 테스트 결과 상세 표시 스크립트
# 성공/실패/에러/스킵 모든 정보를 보여줍니다.

echo "🧪 통합 CMS v2 테스트 실행 중..."
echo "================================="

# Maven 테스트 실행 및 결과 파싱
mvn test 2>&1 | tee /tmp/test_output.log

echo ""
echo "📊 테스트 결과 요약"
echo "================================="

# 전체 테스트 수행 결과 추출
grep "Tests run:" /tmp/test_output.log | while IFS= read -r line; do
    # 각 테스트 클래스별 결과 파싱
    if [[ $line =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+),\ Skipped:\ ([0-9]+).*in\ ([a-zA-Z0-9_.]+) ]]; then
        tests_run=${BASH_REMATCH[1]}
        failures=${BASH_REMATCH[2]}
        errors=${BASH_REMATCH[3]}
        skipped=${BASH_REMATCH[4]}
        class_name=${BASH_REMATCH[5]}
        
        # 성공한 테스트 수 계산
        success=$((tests_run - failures - errors - skipped))
        
        # 결과에 따른 색상 적용
        if [ $failures -eq 0 ] && [ $errors -eq 0 ]; then
            status_color="\033[32m" # 초록색 (성공)
            status_emoji="✅"
        else
            status_color="\033[31m" # 빨간색 (실패)
            status_emoji="❌"
        fi
        
        # 클래스 이름 단축 (패키지 제거)
        short_class=$(echo $class_name | sed 's/.*\.//')
        
        printf "%s %s%-40s\033[0m | " "$status_emoji" "$status_color" "$short_class"
        printf "\033[32m성공: %2d\033[0m | " "$success"
        
        if [ $failures -gt 0 ]; then
            printf "\033[31m실패: %2d\033[0m | " "$failures"
        else
            printf "실패: %2d | " "$failures"
        fi
        
        if [ $errors -gt 0 ]; then
            printf "\033[33m에러: %2d\033[0m | " "$errors"
        else
            printf "에러: %2d | " "$errors"
        fi
        
        if [ $skipped -gt 0 ]; then
            printf "\033[36m스킵: %2d\033[0m\n" "$skipped"
        else
            printf "스킵: %2d\n" "$skipped"
        fi
    fi
done

echo ""
echo "🎯 전체 결과 집계"
echo "================================="

# 전체 결과 집계
total_tests=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*")
total_failures=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Failures: [0-9]*" | grep -o "[0-9]*")
total_errors=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Errors: [0-9]*" | grep -o "[0-9]*")
total_skipped=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Skipped: [0-9]*" | grep -o "[0-9]*")

if [ -n "$total_tests" ]; then
    total_success=$((total_tests - total_failures - total_errors - total_skipped))
    success_rate=$(echo "scale=1; $total_success * 100 / $total_tests" | bc -l)
    
    echo "총 테스트 수: $total_tests"
    echo "✅ 성공: $total_success ($success_rate%)"
    echo "❌ 실패: $total_failures"
    echo "⚠️  에러: $total_errors"
    echo "⏭️  스킵: $total_skipped"
    
    if [ $total_failures -eq 0 ] && [ $total_errors -eq 0 ]; then
        echo ""
        echo "🎉 모든 테스트 성공!"
        echo "================================="
    else
        echo ""
        echo "🔍 실패/에러 상세 정보:"
        echo "================================="
        grep -A 3 "FAILURE\|ERROR" /tmp/test_output.log || echo "상세 정보 없음"
    fi
fi

# 빌드 결과 확인
if grep -q "BUILD SUCCESS" /tmp/test_output.log; then
    echo ""
    echo "🏆 BUILD SUCCESS"
elif grep -q "BUILD FAILURE" /tmp/test_output.log; then
    echo ""
    echo "💥 BUILD FAILURE"
fi

# 임시 파일 정리
rm -f /tmp/test_output.log
