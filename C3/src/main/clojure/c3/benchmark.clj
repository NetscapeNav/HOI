(ns c3.benchmark
  (:require
    [c3.core :refer [parallel-filter]]))

(defn slow-even? [number]
  (Thread/sleep 5)
  (even? number))

(defn -main [& args]
  (try
    (let [numbers (range 200)]
      (println "Последовательный filter:") ; 1145.3259 msec
      (time
        (doall
          (filter slow-even? numbers)))

      (println "Параллельный filter:") ; 299.7816 msec
      (time
        (doall
          (parallel-filter slow-even? numbers 10 4))))
    (finally
      (shutdown-agents))))
