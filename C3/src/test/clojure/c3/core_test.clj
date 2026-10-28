(ns c3.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [c3.core :refer [parallel-filter]]))

(deftest finite-sequence-test
  (testing "Результат совпадает со стандартным filter"
    (let [numbers (range 100)
          predicate #(zero? (mod % 3))
          expected (vec (filter predicate numbers))
          actual (vec (parallel-filter predicate numbers 8 4))]
      (is (= expected actual)))))

(deftest order-test
  (testing "Порядок подходящих элементов сохраняется"
    (is (= [9 8 7 6 5]
           (vec
             (parallel-filter #(> % 4)
                              [9 1 8 2 7 3 6 4 5]
                              2
                              3))))))

(deftest infinite-sequence-test
  (testing "Можно получить конечную часть результата бесконечной последовательности"
    (is (= (vec (range 1 40 2))
           (vec
             (take 20
                   (parallel-filter odd?
                                    (iterate inc 0)
                                    5
                                    3)))))))

(deftest empty-sequence-test
  (testing "Пустая последовательность даёт пустой результат"
    (is (= []
           (vec (parallel-filter even? [] 5 2))))))
