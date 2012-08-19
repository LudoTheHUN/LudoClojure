(ns LudoClojure.test.pperceptron
  (:use [LudoClojure.opencl-utils])
  (:use [LudoClojure.pperceptron])
  (:use [LudoClojure.pperceptron-testhelpers])
  (:use [LudoClojure.utils])
  (:use [clojure.test])
  (:use [calx])
  )

;;(use 'calx)
(def testdata (make_test_array 20 40 3))



(deftest test_manual_pp_test_pp1

(with-out-str (do 
(def pp1 (make_pp {:input_size 5     ;;ERROR frist half of output is incorrect
                   
                   :outputs_size 22
                   :pp_size 30
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.001)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 1.0 )}))

(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))



(pp_readout pp1 :input_data_buf)
(pp_readout pp1 :correct_answer_buf)
(pp_readout pp1 :pp_answer_buf)
(pp_readout pp1 :vecProductResult_buf)
;(count 
  ;(pp_readout pp1 :alpha_buf)
  ;)
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.001)  })
  (pp_readout pp1 :correct_answer_buf)
  (count     (pp_readout pp1 :alpha_buf)    )

(time (dotimes [n 1000]
  (if (= 0 (mod n 1))
    
    (do
       
;      (Thread/sleep 10)
 (lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
  (println n)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 0.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 0.0 -1.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " " 
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 1.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  ;(take 10 (pp_readout pp1 :alpha_buf))
  (if (= 0 (mod n 10))
     (sort (frequencies   (map (fn [x] 
                                 (if  (< x -666.6)
                                   (throw (throw (Exception. "wtf")))
                                 (/ (int (* x 10.0)) 10.0)
                                 )
                                 ) (pp_readout pp1 :alpha_buf) )))
     
     ;(sort (frequencies   (map (fn [x] (/ (int (* x 1000.0)) 1000.0)) (pp_readout pp1 :alpha_buf) )))
  )
  (lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
  ;(pp_readout pp1 :vecProductResult_buf)
  ;(pp_readout pp1 :pp_answer_buf)
  ))
  :done)))



;  0  0  0  0  0  ([-0.9 170] [-0.8 170] [-0.4 166] [-0.3 38] [-0.2 40] [-0.1 39] [0.0 61] [0.1 49] [0.2 42] [0.3 51] [0.4 174] [0.8 163] [0.9 157]) nil991


(time (dotimes [n 100]
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 0.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 0.0 -1.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 1.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
))  ;;500ms
(time (dotimes [n 100]
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 1.0 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 0.0 0.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 0.0 -1.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 1.0 0.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 -1.0 0.0 -1.0 -1.0])
))

))

;; test_manual_pp_test_pp1  , if this false, we did not manage to test the mapping 
(is (= 
    (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
    (pp_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] )
    [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
    ))
    0))

)

(deftest test_manual_pp_test_pp3

(def pp3 (make_pp {:input_size 20     ;;ERROR frist half of output is incorrect
                   
                   :outputs_size 30
                   :pp_size 20
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.001)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.1)    ;;  level of error that is allowed.
                   :mu (float 1.0 )}))

(def testdata3 (make_test_array 
                 20 ;:input_size
                 30 ;:outputs_size
                 3  ;:number of items to learn
                 ))
(pp_print_absolute_error pp3 testdata3)

(train_over_test_data 10 pp3 testdata3)
(train_over_test_data 1000 pp3 testdata3)
(train_over_test_data 10 pp3 testdata3 true)

(pp_abs_errorcounts_each pp3 testdata3 0.3)
(pp_abs_errorcounts_each pp3 testdata3 0.2)

(pp_answer pp3 ((testdata3 0) 0))  ((testdata3 0) 1)
(pp_answer pp3 ((testdata3 1) 0))  ((testdata3 1) 1)
(pp_answer pp3 ((testdata3 2) 0))  ((testdata3 2) 1)

)



(deftest test_manual_pp_test_pp2  ;; Binary only test

(def pp2 (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )}))       ;;  learning modifier around zero   ;0.9




(pp_readout pp2 :input_data_buf)
(pp_readout pp2 :correct_answer_buf)
(pp_readout pp2 :pp_answer_buf)
(pp_readout pp2 :vecProductResult_buf)
;(count 
  ;(pp_readout pp2 :alpha_buf)
  ;)

(def testdata2 (make_test_array 
                 5 ;:input_size
                 3 ;:outputs_size
                 4  ;:number of items to learn
                 ))

(def testdata2
    [
     [[1.0 1.0 1.0 1.0 -1.0] [1.0 1.0 1.0]]
     [[0.0 0.0 0.0 0.0 -1.0] [1.0 -1.0 1.0]]
     [[-1.0 0.0 -1.0 0.0 -1.0] [-1.0 1.0 1.0]]
     [[0.0 1.0 0.0 0.0 -1.0] [-1.0 -1.0 1.0]]])

(train_over_test_data 10 pp2 testdata2 true)
(train_over_test_data 1000 pp2 testdata2 true)

(pp_answer pp2 ((testdata2 0) 0))  ((testdata2 0) 1)
(pp_answer pp2 ((testdata2 1) 0))  ((testdata2 1) 1)
(pp_answer pp2 ((testdata2 2) 0))  ((testdata2 2) 1)

(pp_answer pp2 [1.0 -1.0 -1.0 -1.0 -1.0])


)



