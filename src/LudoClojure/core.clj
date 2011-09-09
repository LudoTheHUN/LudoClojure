(ns LudoClojure.core
  (:use LudoClojure.Win_scrachAlwaysRun)
  (:gen-class))
  
(use 'calx)
(use 'clojure.contrib.math)


  
(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))
  
  

(defn -main [& args]
  (println "Its not easy enough to get started with Clojure programming")
  (println (hello args))
)




(println "hello there from core.clj file")








(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))
(def OpenCLoutputAtom3 (atom 0))
(def OpenCLoutputAtom4 (atom 0))
(def OpenCLoutputAtom5 (atom 0))

(def global_clj_size 4)

(defn testoutputs [global local InnerLoopCount]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
      inputvec_float (vec (for [i (range global_clj_size)] (rand)))]

;;(with-cl (def openCLbuffer (wrap (vec (for [i (range 10000000)] (rand))) :int32)))
;;(with-cl @(enqueue-read openCLbuffer ))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap inputvec_float :float32)
          b (mimic a)
          c (wrap inputvec_float :float32)
          d (mimic a)
          cl_localsize localsize]
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize a b cl_localsize)
	                  (enqueue-barrier)
			          (finish))
               (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))			 
      (swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read a))))
      (swap! OpenCLoutputAtom2 (fn [foo] (deref (enqueue-read b))))
      (release! a)
      (release! b)
    nil)))

(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(if (> 8 (count inputvec_float))
    (println " " @OpenCLoutputAtom1 "\n " @OpenCLoutputAtom2)
	(println "too big to print,"

	                                "\n CLin is        :" (@OpenCLoutputAtom1 (- global_clj_size 1)) 
	                                "\n CLin target    :" (* (@OpenCLoutputAtom1 (- global_clj_size 1)) (@OpenCLoutputAtom1 (- global_clj_size 1)))
									"\n CLout          :" (@OpenCLoutputAtom2 (- global_clj_size 1))
									"\n orgin Input    :" (inputvec_float (- global_clj_size 1))
									"\n orgin Output   :" (* (inputvec_float (- global_clj_size 1)) (inputvec_float (- global_clj_size 1)))
									"\n Problemsize    :" (count @OpenCLoutputAtom1)
									))
									
(def startnanotime_clj (. System (nanoTime)))
(def testOpenCl_vs_clj(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (* (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom1 x)) )))
 [] (range 0 global_clj_size)))
(def endnanotime_clj (. System (nanoTime)))
(println "testing output: " testOpenCl_vs_clj )


(println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
(println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")

		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7

 
