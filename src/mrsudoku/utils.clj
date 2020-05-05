
(ns mrsudoku.utils
  (:use midje.sweet))

(defn concatv
  "Concatenate vectors."
  ([] [])
  ([v] v)
  ([v1 v2] (into v1 v2))
  ([v1 v2 & more]
   (into v1 (apply concat (cons v2 more)))))

(fact (concatv []) => [])

(fact (concatv [1 2]) => [1 2])

(fact (concatv [1 2] [3 4 5]) => [1 2 3 4 5])

(fact (concatv [1 2] [3 4 5] [6 7]) => [1 2 3 4 5 6 7])




