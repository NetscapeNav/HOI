(ns c2.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [c2.core :refer [primes]]))

(deftest first-ten-primes-test
  (testing "Первые десять простых чисел"
    (is (= [2 3 5 7 11 13 17 19 23 29]
           (vec (take 10 primes))))))

(deftest hundredth-prime-test
  (testing "Сотое простое число"
    (is (= 541
           (nth primes 99)))))