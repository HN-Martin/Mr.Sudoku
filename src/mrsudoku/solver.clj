(ns mrsudoku.solver
  (:use midje.sweet)
  (:require [mrsudoku.grid :as g]
            [mrsudoku.engine :as e]
            [clojure.set :as s]))

(defn block-pos
  "Gives the position of the block containing the cell of coordinate '(cx,cy)' "
  [cx cy]
  (+ (* 3 (quot (dec cy) 3)) (inc (quot (dec cx) 3))))

(fact 
 "La case (2,3) est dans le bloc 1, la (7,4) dans le bloc 6"
 (block-pos 2 3) => 1
 (block-pos 7 4) => 6)


(defn init-grid
  "Changes the empty values to a set of possible values in grid"
  [grid]
  (let [all #{1 2 3 4 5 6 7 8 9}]
    (loop [g grid cx 1 cy 1]
      (cond
        (= cy 10) g
        (= cx 10) (recur  g 1 (inc cy))
        (not (g/cell-value (g/cell grid cx cy))) (recur (g/change-cell g cx cy (g/mk-cell (s/difference all (e/values (g/col grid cx)) (e/values (g/row grid cy)) (e/values (g/block grid (block-pos cx cy)))))) (inc cx) cy)
        :else (recur g (inc cx) cy)))))

(fact
 (g/block (init-grid sudoku-grid) 1) => [{:status :init, :value 5}
                                         {:status :init, :value 3}
                                         {:status :init, :value #{1 4 2}}
                                         {:status :init, :value 6}
                                         {:status :init, :value #{7 4 2}}
                                         {:status :init, :value #{7 4 2}}
                                         {:status :init, :value #{1 2}}
                                         {:status :init, :value 9}
                                         {:status :init, :value 8}]
 
 (g/row (init-grid sudoku-grid) 1) => [{:status :init, :value 5}
                                       {:status :init, :value 3}
                                       {:status :init, :value #{1 4 2}}
                                       {:status :init, :value #{6 2}}
                                       {:status :init, :value 7}
                                       {:status :init, :value #{4 6 2 8}}
                                       {:status :init, :value #{1 4 9 8}}
                                       {:status :init, :value #{1 4 2 9}}
                                       {:status :init, :value #{4 2 8}}]
 
 (g/col (init-grid sudoku-grid) 1) => [{:status :init, :value 5}
                                       {:status :init, :value 6}
                                       {:status :init, :value #{1 2}}
                                       {:status :init, :value 8}
                                       {:status :init, :value 4}
                                       {:status :init, :value 7}
                                       {:status :init, :value #{1 3 9}}
                                       {:status :init, :value #{3 2}}
                                       {:status :init, :value #{1 3 2}}])

(defn next-cell
  "Choose the next cell in 'grid' that has to be set"
  [grid]
  (loop [nx -1 ,ny -1 ,cx 1 ,cy 1 ,min 9]
      (cond
        (= cy 10) (vector nx ny)
        (= cx 10) (recur  nx ny 1 (inc cy) min)
        (not (g/cell-value (g/cell grid cx cy))) (let [ v (count (:value (g/cell grid cx cy)))]
                                                   (cond 
                                                     (= 1 v) (vector cx cy)
                                                     (< v min)(recur cx cy (inc cx) cy v)
                                                     :else (recur nx ny (inc cx) cy min)))
        :else (recur nx ny (inc cx) cy min))))

(fact 
 (next-cell (init-grid sudoku-grid)) => [5 5])

(defn change-block
  "Remove the value 'val from cells with a set of values
   in the block containing the cell with coordinate (cx, cy) "
  [grid cx cy val]
  (let [cb (quot (dec cx) 3) lb (quot (dec cy) 3)]
   (loop [g grid i 1]
         (let [c (e/coord cb lb i) x (first c) y (second c)]
          (cond 
           (= i 10) g
           (not (g/cell-value (g/cell grid x y))) (let [cell (g/cell grid x y) v (get cell :value)]
                                    (recur (g/change-cell g x y (g/mk-cell (disj v val))) (inc i)))
           :else (recur g (inc i)) )))))

(fact
 (g/block (change-block (init-grid sudoku-grid) 1 1 2 ) 1) => [{:status :init, :value 5}
                                                              {:status :init, :value 3}
                                                              {:status :init, :value #{1 4}}
                                                              {:status :init, :value 6}
                                                              {:status :init, :value #{7 4}}
                                                              {:status :init, :value #{7 4}}
                                                              {:status :init, :value #{1}}
                                                              {:status :init, :value 9}
                                                              {:status :init, :value 8}])


(defn change-col
  "Remove the value 'val from cells with a set of values
   in the cx 'th column"
  [grid cx val]
  (loop [g grid i 1]  
      (cond
        (= i 10) g
        (not (g/cell-value (g/cell grid cx i))) (let [cell (g/cell grid cx i) v (:value cell)]
                                 (recur (g/change-cell g cx i (g/mk-cell (disj v val))) (inc i)))
        :else (recur g (inc i)))))

(fact 
 (g/col (change-col (init-grid sudoku-grid) 1 1) 1) => [{:status :init, :value 5}
                                                        {:status :init, :value 6}
                                                        {:status :init, :value #{2}}
                                                        {:status :init, :value 8}
                                                        {:status :init, :value 4}
                                                        {:status :init, :value 7}
                                                        {:status :init, :value #{3 9}}
                                                        {:status :init, :value #{3 2}}
                                                        {:status :init, :value #{3 2}}])


(defn change-row
  "Remove the value 'val from cells with a set of values
   in the cy 'th row"
  [grid cy val]
  (loop [g grid i 1]
      (cond
        (= i 10) g
        (not (g/cell-value (g/cell grid i cy))) (let [cell (g/cell grid i cy) v (:value cell)]
                                                 (recur (g/change-cell g i cy (g/mk-cell (disj v val))) (inc i)))
        :else (recur g (inc i)))))

(fact
 (g/row (change-row (init-grid sudoku-grid) 1 4) 1) => [{:status :init, :value 5}
                                                        {:status :init, :value 3}
                                                        {:status :init, :value #{1 2}}
                                                        {:status :init, :value #{6 2}}
                                                        {:status :init, :value 7}
                                                        {:status :init, :value #{6 2 8}}
                                                        {:status :init, :value #{1 9 8}}
                                                        {:status :init, :value #{1 2 9}}
                                                        {:status :init, :value #{2 8}}])

(defn set-cell 
  "Change the 'grid' cell at coordinate '(cx,cy)' 
   as well as the other cells in it's row , block and column"
  [grid cx cy] 
    (let [val (first (:value (g/cell grid cx cy)))]
     (g/change-cell (change-block (change-row (change-col grid cx val) cy val) cx cy val) cx cy (g/mk-cell val))))

(fact 
 (g/block (set-cell (init-grid sudoku-grid) 5 5) 5) => [{:status :init, :value #{7 9}}
                                                        {:status :init, :value 6}
                                                        {:status :init, :value #{7 1 4}}
                                                        {:status :init, :value 8}
                                                        {:status :init, :value 5}
                                                        {:status :init, :value 3}
                                                        {:status :init, :value #{9}}
                                                        {:status :init, :value 2}
                                                        {:status :init, :value #{1 4}}])


(defn solve [grid] 
  (loop [gridsolved (init-grid grid)]
      (let [ cnext (next-cell gridsolved), x (first cnext) y (second cnext)]
        (if (pos? x)
          (recur (set-cell gridsolved x y))
          gridsolved))))
 
