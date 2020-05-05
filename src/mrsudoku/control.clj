
(ns mrsudoku.control
  (:require
   [mrsudoku.grid :as g]
   [mrsudoku.view :as v]
   [mrsudoku.engine :as e]
   [clojure.pprint :as pp]
   [seesaw.core :refer [alert text! invoke-later select]]))

(defn mk-grid-controller
  [grid] (atom {:grid grid}))

(defn change-cell! [ctrl cx cy cell-widget cell]
  (swap! ctrl #(assoc % :grid (g/change-cell (:grid %) cx cy cell)))
  (invoke-later (v/update-cell-view! cell cell-widget))
  (let [ngrid (:grid (deref ctrl))]
    (println (g/grid->str ngrid))))

(defn fetch-cell-widget [ctrl cx cy]
  (let [id-widget (keyword (str "#cell-" cx "-" cy))]
    (if-let [cell-widget (select (:grid-widget (deref ctrl)) [id-widget])]
      cell-widget
      (throw (ex-info "Widget not found" {:cell-x cx :cell-y cy})))))

(defn update-conflicts! [ctrl]
  (let [conflicts (e/grid-conflicts (:grid (deref ctrl)))]
    (println "conflicts = " conflicts)
    (dorun (map (fn [[[cx cy] conflict]]
                  (change-cell! ctrl cx cy (fetch-cell-widget ctrl cx cy) conflict))
                conflicts))))

(defn reset-conflicts! [ctrl]
  (g/do-grid (fn [cx cy cell]
               ;;(println (str "[" cx "," cy "]: " cell))
               (if (= (:status cell) :conflict)
                 (change-cell! ctrl cx cy (fetch-cell-widget ctrl cx cy) {:status :set :value (:value cell)})))
             (:grid (deref ctrl))))

(defn cell-validate-input! [ctrl cell-widget cx cy val]
  ;; first, set the value
  (change-cell! ctrl cx cy cell-widget {:status :set, :value val})
  ;; then, compute the conflicts
  ;; TODO : reset (old) conflicts
  (reset-conflicts! ctrl)
  ;; Then compute the nuew conflicts
  (update-conflicts! ctrl))

(defn cell-clear! [ctrl cell-widget cx cy]
  (change-cell! ctrl cx cy cell-widget {:status :empty})
  (reset-conflicts! ctrl)
  (update-conflicts! ctrl))

(defn cell-input-handler [ctrl cell-widget cell-x cell-y]
  (fn [cell-event]
    (let [doc (.getDocument cell-event)
          input (.getText doc 0 (.getLength doc))]
      ;;(print (str "at cell (" cell-x "," cell-y "): "))
      ;;(println input)
      (if-let [val (and (not= input "")
                        (try (let [val (Integer/parseInt input)]
                               (when (<= 1 val 9)
                                 val))
                             (catch Exception _ nil)))]
        (cell-validate-input! ctrl cell-widget cell-x cell-y val)
        (do
          (cell-clear! ctrl cell-widget cell-x cell-y)
          (invoke-later (text! cell-widget "")))))))



