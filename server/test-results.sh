#!/bin/bash

# Unified CMS v2 Test Results Analysis Script
# Displays comprehensive test results with modern visual elements

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                           UNIFIED CMS v2 TEST SUITE                          ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Maven test execution with result parsing
mvn test 2>&1 | tee /tmp/test_output.log

echo ""
echo "┌─────────────────────────────────────────────────────────────────────────────┐"
echo "│                              TEST RESULTS SUMMARY                           │"
echo "└─────────────────────────────────────────────────────────────────────────────┘"
echo ""
printf "  \033[1m%-37s\033[0m │ \033[1;92mPASS\033[0m │ \033[1;91mFAIL\033[0m │ \033[1;93mERR\033[0m │ \033[1;96mSKIP\033[0m │ \033[1mSTATUS\033[0m\n"
echo "  ─────────────────────────────────────┼──────┼──────┼─────┼──────┼────────"

# Parse individual test class results
grep "Tests run:" /tmp/test_output.log | while IFS= read -r line; do
    if [[ $line =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+),\ Skipped:\ ([0-9]+).*in\ ([a-zA-Z0-9_.]+) ]]; then
        tests_run=${BASH_REMATCH[1]}
        failures=${BASH_REMATCH[2]}
        errors=${BASH_REMATCH[3]}
        skipped=${BASH_REMATCH[4]}
        class_name=${BASH_REMATCH[5]}
        
        # Calculate successful tests
        success=$((tests_run - failures - errors - skipped))
        
        # Determine status with modern indicators
        if [ $failures -eq 0 ] && [ $errors -eq 0 ]; then
            status_indicator="●"
            status_color="\033[1;92m" # Bright green
            status_text="PASS"
        else
            status_indicator="●"
            status_color="\033[1;91m" # Bright red  
            status_text="FAIL"
        fi
        
        # Extract class name (remove package)
        short_class=$(echo $class_name | sed 's/.*\.//')
        
        # Display result with modern formatting aligned to header
        printf "  %s%s\033[0m \033[1m%-35s\033[0m │ " "$status_color" "$status_indicator" "$short_class"
        printf "\033[92m %3d\033[0m │ " "$success"
        
        if [ $failures -gt 0 ]; then
            printf "\033[91m %3d\033[0m │ " "$failures"
        else
            printf "\033[90m %3d\033[0m │ " "$failures"
        fi
        
        if [ $errors -gt 0 ]; then
            printf "\033[93m %2d\033[0m │ " "$errors"
        else
            printf "\033[90m %2d\033[0m │ " "$errors"
        fi
        
        if [ $skipped -gt 0 ]; then
            printf "\033[96m %3d\033[0m │ " "$skipped"
        else
            printf "\033[90m %3d\033[0m │ " "$skipped"
        fi
        
        printf "%s%s\033[0m\n" "$status_color" "$status_text"
    fi
done

echo ""
echo "┌─────────────────────────────────────────────────────────────────────────────┐"
echo "│                              OVERALL STATISTICS                             │"
echo "└─────────────────────────────────────────────────────────────────────────────┘"

# Aggregate total results
total_tests=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*")
total_failures=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Failures: [0-9]*" | grep -o "[0-9]*")
total_errors=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Errors: [0-9]*" | grep -o "[0-9]*")
total_skipped=$(grep "Tests run:" /tmp/test_output.log | tail -1 | grep -o "Skipped: [0-9]*" | grep -o "[0-9]*")

if [ -n "$total_tests" ]; then
    total_success=$((total_tests - total_failures - total_errors - total_skipped))
    success_rate=$(echo "scale=1; $total_success * 100 / $total_tests" | bc -l)
    
    # Modern metrics display
    echo ""
    printf "  \033[1;96m◆\033[0m Total Tests      : \033[1m%s\033[0m\n" "$total_tests"
    printf "  \033[1;92m◆\033[0m Passed          : \033[1;92m%s\033[0m \033[90m(%s%%)\033[0m\n" "$total_success" "$success_rate"
    printf "  \033[1;91m◆\033[0m Failed          : \033[1;91m%s\033[0m\n" "$total_failures"
    printf "  \033[1;93m◆\033[0m Errors          : \033[1;93m%s\033[0m\n" "$total_errors"
    printf "  \033[1;96m◆\033[0m Skipped         : \033[1;96m%s\033[0m\n" "$total_skipped"
    echo ""
    
    if [ $total_failures -eq 0 ] && [ $total_errors -eq 0 ]; then
        echo "╔══════════════════════════════════════════════════════════════════════════════╗"
        echo "║                          ✓ ALL TESTS PASSED                                  ║"
        echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    else
        echo "┌─────────────────────────────────────────────────────────────────────────────┐"
        echo "│                            FAILURE DETAILS                                  │"
        echo "└─────────────────────────────────────────────────────────────────────────────┘"
        grep -A 3 "FAILURE\|ERROR" /tmp/test_output.log || echo "  No detailed information available"
    fi
fi

# Build status check
echo ""
if grep -q "BUILD SUCCESS" /tmp/test_output.log; then
    echo "┌─────────────────────────────────────────────────────────────────────────────┐"
    echo "│                         ⬢ BUILD SUCCESSFUL                                  │"
    echo "└─────────────────────────────────────────────────────────────────────────────┘"
elif grep -q "BUILD FAILURE" /tmp/test_output.log; then
    echo "┌─────────────────────────────────────────────────────────────────────────────┐"
    echo "│                           ⬢ BUILD FAILED                                    │"
    echo "└─────────────────────────────────────────────────────────────────────────────┘"
fi

echo ""

# Cleanup temporary files
rm -f /tmp/test_output.log
