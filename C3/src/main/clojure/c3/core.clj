(ns c3.core)

(defn make-blocks [collection block-size]
  (partition-all block-size collection))

(defn filter-block [predicate block]
  (doall (filter predicate block)))

(defn start-filter-block [predicate block]
  (future (filter-block predicate block)))

(defn start-filter-batch [predicate blocks parallelism]
  (doall (map #(start-filter-block predicate %) (take parallelism blocks))))

(defn collect-filter-results [jobs]
  (mapcat deref jobs))

(defn process-blocks [predicate blocks parallelism]
  (lazy-seq
    (when (seq blocks)
      (let [jobs (start-filter-batch predicate blocks parallelism)
            results (collect-filter-results jobs)
            remaining-blocks (drop parallelism blocks)]
        (concat
          results
          (process-blocks predicate remaining-blocks parallelism))))))

(defn parallel-filter [predicate collection block-size parallelism]
  (let [blocks (make-blocks collection block-size)]
    (process-blocks predicate blocks parallelism)))

(defn -main [& args]
  (println
    (take 20
          (parallel-filter even? (iterate inc 0) 3 4)))
  (shutdown-agents))
