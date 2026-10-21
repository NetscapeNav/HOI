(ns c2.test-runner
  (:require
    [clojure.test :as test]
    [c2.core-test]))

(defn -main [& args]
  (let [result (test/run-tests 'c2.core-test)
        failures (+ (:fail result) (:error result))]
    (when (pos? failures)
      (System/exit 1))))