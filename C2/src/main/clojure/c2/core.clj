(ns c2.core)

(def whole
  (iterate inc 2))

(defn eratosphen [numbers]
  (lazy-seq
    (let [prime (first numbers)
      remaining (rest numbers)
      filtered (remove #(zero? (mod % prime)) remaining)]
      (cons prime (eratosphen filtered)))))

(def primes
  (eratosphen whole))

(defn -main [& args]
  (println (take 10 primes)))
