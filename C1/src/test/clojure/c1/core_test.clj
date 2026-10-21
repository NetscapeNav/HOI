(ns c1.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [c1.core :refer [generate-strings]]))

(deftest example-from-task-test
  (testing "Алфавит из трёх букв и строки длины 2"
    (is (= #{"ab" "ac" "ba" "bc" "ca" "cb"}
           (set (generate-strings ["a" "b" "c"] 2))))))

(deftest zero-length-test
  (testing "Для длины 0 существует одна пустая строка"
    (is (= [""]
           (vec (generate-strings ["a" "b" "c"] 0))))))

(deftest result-count-test
  (testing "Для трёх букв и длины 3 получается 3 * 2 * 2 строк"
    (is (= 12
           (count (generate-strings ["a" "b" "c"] 3))))))
