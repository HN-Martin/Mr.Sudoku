
(ns mrsudoku.grid
  (:use midje.sweet)
  (:require [mrsudoku.utils :refer [concatv]]))

(defn mk-cell
  "Create a new cell."
  ([] {:status :empty})
  ([v] {:status :init, :value v})
  ([st v] {:status st, :value v}))

(defn cell-value [cell]
  (case (:status cell)
    (:init :set :solved :conflict) (:value cell)
    nil))

(fact
 (cell-value {:status :init, :value 5}) => 5)

(fact
 (cell-value {:status :empty}) => nil)


;;; Remark: cell status can be :
;;;   - :init value set at initialization
;;;   - :empty
;;;   - :set value set by the player
;;;   - :conflict  value is conflicting
;;;   - :solved    value is solved by a solver algorithm

(def ^:private sudoku-grid
  [[;; row 1
    [(mk-cell 5) (mk-cell 3) (mk-cell)
     (mk-cell 6) (mk-cell) (mk-cell)
     (mk-cell) (mk-cell 9) (mk-cell 8)]
    [(mk-cell) (mk-cell 7) (mk-cell)
     (mk-cell 1) (mk-cell 9) (mk-cell 5)
     (mk-cell) (mk-cell) (mk-cell)]
    [(mk-cell) (mk-cell) (mk-cell)
     (mk-cell) (mk-cell) (mk-cell)
     (mk-cell) (mk-cell 6) (mk-cell)] ],
   [;; row 2
    [(mk-cell 8) (mk-cell) (mk-cell)
     (mk-cell 4) (mk-cell) (mk-cell)
     (mk-cell 7) (mk-cell) (mk-cell)]
    [(mk-cell) (mk-cell 6) (mk-cell)
     (mk-cell 8) (mk-cell) (mk-cell 3)
     (mk-cell) (mk-cell 2) (mk-cell)]
    [(mk-cell) (mk-cell) (mk-cell 3)
     (mk-cell) (mk-cell) (mk-cell 1)
     (mk-cell) (mk-cell) (mk-cell 6)]],
   [;; row 3
    [(mk-cell) (mk-cell 6) (mk-cell)
      (mk-cell) (mk-cell) (mk-cell)
     (mk-cell) (mk-cell) (mk-cell)]
    [(mk-cell) (mk-cell) (mk-cell)
     (mk-cell 4) (mk-cell 1) (mk-cell 9)
     (mk-cell) (mk-cell 8) (mk-cell)]
    [(mk-cell 2) (mk-cell 8) (mk-cell)
     (mk-cell) (mk-cell) (mk-cell 5)
     (mk-cell) (mk-cell 7) (mk-cell 9)]]])


(defn cell
  "Get the cell at coordinates `(cx,cy)`
with `cx` the column number and `cy` the row number."
  [grid cx cy]
  {:pre [(<= 1 cx 9)
         (<= 1 cy 9)]}
  (let [block-x (quot (- cx 1) 3)
        cell-x (rem (- cx 1) 3)
        block-y (quot (- cy 1) 3)
        cell-y (rem (- cy 1) 3)
        cell-ref (+ (* cell-y 3) cell-x)]
    ;(println (str "block-x=" block-x ", block-y=" block-y))
    ;(println (str "cell-x=" cell-x ", cell-y=" cell-y))
    (let [block ((grid block-y) block-x)]
      ;(println (str "block=" block))
      (block cell-ref))))

(fact (cell sudoku-grid 1 1) => {:status :init, :value 5})

(fact (cell sudoku-grid 2 1) => {:status :init, :value 3})

(fact (cell sudoku-grid 3 1) => {:status :empty})

(fact (cell sudoku-grid 1 2) => {:status :init, :value 6})

(fact (cell sudoku-grid 5 2) => {:status :init, :value 9})

(fact (cell sudoku-grid 5 6) => {:status :init, :value 2})

(fact (cell sudoku-grid 9 9) => {:status :init, :value 9})

(defn change-cell
  "Change the `grid` cell at coordinates `(cx,cy)`
  with `cx` the column number and `cy` the row number,
  and `cell` is the new cell-content."
  [grid cx cy cell]
  {:pre  [(<= 1 cx 9)
          (<= 1 cy 9)]}
  (let [block-x (quot (- cx 1) 3)
        cell-x (rem (- cx 1) 3)
        block-y (quot (- cy 1) 3)
        cell-y (rem (- cy 1) 3)
        cell-ref (+ (* cell-y 3) cell-x)]
    (update-in grid [block-y block-x cell-ref] (fn [_] cell))))

(fact
 (cell (change-cell sudoku-grid 1 1 {:status :set, :value 4}) 1 1)
 => {:status :set, :value 4})

(defn cell->str [cell]
  (condp = (:status cell)
    :init (str " " (:value cell) " ")
    :empty " . "
    :set (str "[" (:value cell) "]")
    :conflict (str "!" (:value cell) "!")))

(fact
 (cell->str (cell sudoku-grid 1 1))
 => " 5 ")

(fact
 (cell->str (cell sudoku-grid 1 3))
 => " . ")

(defn block
  "Get the #`b` block of the `grid`."
  [grid b]
  {:pre [(<= 1 b 9)]}
  (let [row (quot (- b 1) 3)
        blk (rem (- b 1) 3)]
    (nth (nth grid row) blk)))

(fact
 (block sudoku-grid 1)
 => [{:status :init, :value 5}
     {:status :init, :value 3}
     {:status :empty}
     {:status :init, :value 6}
     {:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :init, :value 9}
     {:status :init, :value 8}])

(fact
 (block sudoku-grid 8)
 => [{:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :init, :value 4}
     {:status :init, :value 1}
     {:status :init, :value 9}
     {:status :empty}
     {:status :init, :value 8}
     {:status :empty}])


(defn reduce-block
  "Reduce the `b`-th `block` of a sudoku grid with a function taking 4
  parameters: `acc` for the accumulated value, `index` for the cell
  index in the block, `cx, cy` for the cell
  coordinates in the grid and `cell` for the cell content."  
  [f init block b]
  (let [bx (* 3 (rem (- b 1) 3))
        by (* 3 (quot (- b 1) 3))]
    (loop [yoffset 0, index 1, cells block, acc init]
      (if (= yoffset 3)
        acc
        (let [[nacc, ncells, nindex]
              (loop [xoffset 0, index index, cells cells, acc acc]
                (if (= xoffset 3)
                  [acc, cells, index]
                  (recur (+ xoffset 1) (+ index 1)
                         (rest cells) (f acc
                                         index
                                         (+ bx xoffset 1)
                                         (+ by yoffset 1)
                                         (first cells)))))]
          (recur (+ yoffset 1) nindex ncells nacc))))))

(fact
 (reduce-block (fn [acc index cx cy cell]
                 (conj acc [index, [cx,cy]])) [] (block sudoku-grid 1) 1)
 => [[1 [1 1]] [2 [2 1]] [3 [3 1]] [4 [1 2]] [5 [2 2]] [6 [3 2]] [7 [1 3]] [8 [2 3]] [9 [3 3]]])

(fact
 (reduce-block (fn [acc index cx cy cell]
                 (conj acc (cell-value cell))) [] (block sudoku-grid 1) 1)
 => [5 3 nil 6 nil nil nil 9 8])

(fact
 (reduce-block (fn [acc index cx cy cell]
                 (conj acc [cx,cy])) [] (block sudoku-grid 8) 8)
 => [[4 7] [5 7] [6 7] [4 8] [5 8] [6 8] [4 9] [5 9] [6 9]])

(defn do-block
  "Do the effects in function `f!` while traversing the `b`-th `block`
  of a sudoku grid . The function `f!` takes 4 parameters: `index` for
  the index of the cell in the block, `cx, cy`
  for the cell coordinates in the grid and `cell` for the cell content."
  [f! block b]
  (reduce-block (fn [_ index cx cy cell]
                  (f! index cx cy cell)) nil block b))

(fact
 (let [cnt (atom 0)]
   (do-block (fn [index cx cy cell]
               (when (= (:status cell) :empty)
                 (swap! cnt #(+ % 1)))) (block sudoku-grid 1) 1)
   @cnt) => 4)

(defn block-row
  "Get the #`r` row in the `grid`."
  [block r]
  {:pre [(<= 1 r 3)]}
  (let [base (* (- r 1) 3)]
    [(nth block base) (nth block (+ base 1)) (nth block (+ base 2))]))

(fact
 (block-row (nth (nth sudoku-grid 0) 0) 1)
 => [{:status :init, :value 5} {:status :init, :value 3} {:status :empty}])

(fact
 (block-row (nth (nth sudoku-grid 0) 0) 2)
 => [{:status :init, :value 6} {:status :empty} {:status :empty}])

(fact
 (block-row (nth (nth sudoku-grid 0) 0) 3)
 => [{:status :empty} {:status :init, :value 9} {:status :init, :value 8}])

(defn row
  "Get the #`r` row in the `grid`."
  [grid r]
  {:pre [(<= 1 r 9)]}
  (let [block-r (quot (- r 1) 3)
        row-r (+ (rem (- r 1) 3) 1)
        blocks (nth grid block-r)]
    (concatv (block-row (nth blocks 0) row-r)
             (block-row (nth blocks 1) row-r)
             (block-row (nth blocks 2) row-r))))

(fact
 (row sudoku-grid 1)
 => [{:status :init, :value 5}
     {:status :init, :value 3}
     {:status :empty}
     {:status :empty}
     {:status :init, :value 7}
     {:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :empty}])

(fact
 (row sudoku-grid 7)
 => [{:status :empty}
     {:status :init :value 6}
     {:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :init, :value 2}
     {:status :init, :value 8}
     {:status :empty}])

(defn row->str [row]
  (clojure.string/join " " (map cell->str row)))


(fact
 (row->str (row sudoku-grid 1))
 => " 5   3   .   .   7   .   .   .   . ")

(defn rows
  "Fetch all the rows of a sudoku `grid`."
  [grid]
  (for [i (range 1 10)]
    (row grid i)))

(fact
 (count (rows sudoku-grid)) => 9)

(defn grid->str [grid]
  (clojure.string/join "\n" (map row->str (rows grid))))

(fact
 (grid->str sudoku-grid)
 => " 5   3   .   .   7   .   .   .   . \n 6   .   .   1   9   5   .   .   . \n .   9   8   .   .   .   .   6   . \n 8   .   .   .   6   .   .   .   3 \n 4   .   .   8   .   3   .   .   1 \n 7   .   .   .   2   .   .   .   6 \n .   6   .   .   .   .   2   8   . \n .   .   .   4   1   9   .   .   5 \n .   .   .   .   8   .   .   7   9 ")

(defn col
  "Get the #`c` column of the `grid`."
  [grid c]
  {:pre [(<= 1 c 9)]}
  (into [] (for [cy (range 1 10)]
             (cell grid c cy))))

(fact
 (col sudoku-grid 1)
 => [{:status :init, :value 5}
     {:status :init, :value 6}
     {:status :empty}
     {:status :init, :value 8}
     {:status :init, :value 4}
     {:status :init, :value 7}
     {:status :empty}
     {:status :empty}
     {:status :empty}])

(fact
 (col sudoku-grid 8)
 => [{:status :empty}
     {:status :empty}
     {:status :init, :value 6}
     {:status :empty}
     {:status :empty}
     {:status :empty}
     {:status :init, :value 8}
     {:status :empty}
     {:status :init, :value 7}])


(defn reduce-grid
  "Reduce the whole `grid` of a sudoku with a function taking 4
  parameters: `acc` for the accumulated value, `cx, cy` for the cell
  coordinates and `cell` for the cell content."
  [f init grid]
  (loop [cy 1, acc init]
    (if (= cy 10)
      acc
      (let [nacc (loop [cx 1, acc acc]
                   (if (= cx 10)
                     acc
                     (recur (+ cx 1) (f acc cx cy (cell grid cx cy)))))]
        (recur (+ cy 1) nacc)))))


(fact ;; counting empty cells
 (reduce-grid (fn [acc cx cy cell]
                (if (= (:status cell) :empty)
                  (+ acc 1)
                  acc)) 0 sudoku-grid)
 => 51)


(defn do-grid
  "Do the effects in function `f!` while traversing the `grid` of a
  sudoku. The function `f!` takes 3 parameters: `cx, cy` for the cell
  coordinates and `cell` for the cell content."
  [f! grid]
  (reduce-grid (fn [_ cx cy cell]
                 (f! cx cy cell)) nil grid))

(fact
 (let [cnt (atom 0)]
   (do-grid (fn [cx cy cell]
              (when (= (:status cell) :empty)
                (swap! cnt #(+ % 1)))) sudoku-grid)
   @cnt) => 51)


