(ns c1.core)

(defn extend-word [word alphabet]
  (map
    #(str word %)
    (remove #(.endsWith word %) alphabet)))

(defn extend-all [words alphabet]
  (reduce concat (map #(extend-word % alphabet) words)))

(defn generate-strings [alphabet n]
  (reduce (fn [words _] (extend-all words alphabet)) [""] (range n)))

(defn -main [& args]
  (println (generate-strings ["a" "b" "c"] 2)))