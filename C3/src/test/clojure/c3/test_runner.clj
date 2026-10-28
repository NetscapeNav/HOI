(ns c3.test-runner
  (:require
    [clojure.test :as test]
    [c3.core-test]))

(defn -main [& args]
  (let [result (try
                 (test/run-tests 'c3.core-test)
                 (finally
                   (shutdown-agents)))
        failures (+ (:fail result) (:error result))]
    (when (pos? failures)
      (System/exit 1))))
