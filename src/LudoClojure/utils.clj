(ns LudoClojure.utils
)

(println "loading utils")

(defn make_random_float_array [size booster seed]
   (doall
   (let [r (java.util.Random. seed)]
     (loop [i 0 outlist []]
       (if (= i size)
         outlist
         (recur (inc i) (conj outlist (float (+ (* (float (.nextInt r 100)) 0.01) booster)) )))))))


(defn make_test_array [sizein sizeout numberof]  ;;TODO make this take a pp so that a test for it can be created on the fly
" Makes an array of test data instances. Output is of the form 
  [
    [sizein_array sizeout_array]
    ....
    [sizein_array sizeout_array] ]
"
(reduce (fn [col val]
(conj col
[(conj  (make_random_float_array (- sizein 1) -0.5 (+ val 1000)) (float -1.0))
 (make_random_float_array sizeout -0.5 val)]   )) []  (range numberof))
)


(defn round [s n] 
  (.setScale (bigdec n) s java.math.RoundingMode/HALF_EVEN))

(defn abs "(abs n) is the absolute value of n" [n]
  (cond
   (not (number? n)) (throw (IllegalArgumentException.
           "abs requires a number"))
   (neg? n) (* -1 n)
   :else n))






