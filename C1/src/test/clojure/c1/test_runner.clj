(ns c1.test-runner
  (:require
    [clojure.test :as test]
    [c1.core-test]))

(defn -main [& args]
  (let [result (test/run-tests 'c1.core-test)
        failures (+ (:fail result) (:error result))]
    (when (pos? failures)
      (System/exit 1))))
