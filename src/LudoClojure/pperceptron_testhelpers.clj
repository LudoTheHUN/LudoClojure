(ns LudoClojure.pperceptron-testhelpers
  (:use [LudoClojure.pperceptron])
  (:use [LudoClojure.utils])
  )

(println "loading pperceptron-testhelpers")

;;Test and data loading helper functions



(defn pp_abs_errorcounts [pp testdata_point allower_error]
     (reduce + (map (fn [x y]
       (if (< (abs (- (float (round 1 x)) (float (round 1  y)))) allower_error)
       0 1))
  (pp_answer pp (testdata_point 0))  (testdata_point 1))))
  

(defn pp_abs_errorcounts_each [pp testdata allower_error]
  (let [n (count testdata)]
     (map (fn [k] (pp_abs_errorcounts pp (testdata k) allower_error))  (range n))))





(defn pp_print_absolute_error [pp testdata]
(dotimes [n (count testdata)]
 (print " " (reduce + (map (fn [x y]
                           ;;;ARGHH give me an abs  
                            (round 4  (abs (- (float (round 2 x)) (float (round 2  y))))))
  (pp_answer pp ((testdata n) 0))  ((testdata n) 1))))
  ))



(defn train_over_test_data [k pp testdata & verbose?]
 (time (dotimes [n k]

  (dotimes [m (count testdata)] 
      (pp_train_and_answer pp ((testdata m) 0) ((testdata m) 1) {:gama (float 0.4) :eta (float 0.0001)  }))
  (if verbose?
  (do (print  (pp_print_absolute_error pp testdata)
                                         " iter:" n 
                                         " Er0.3:" (pp_abs_errorcounts_each pp testdata 0.3)
                                         " Er0.2:" (pp_abs_errorcounts_each pp testdata 0.2)
                                         " Er0.1:" (pp_abs_errorcounts_each pp testdata 0.1)
                                         " Er0.0:" (pp_abs_errorcounts_each pp testdata 0.0))
  (println " ::")))
)))
