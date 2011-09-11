(ns LudoClojure.core
  (:use LudoClojure.Win_scrachAlwaysRun)
  (:require calx)  ;;this means that I don't haveto have a copy of the calx source in src
 
  (:require clojure.contrib.duck-streams )   ;needed for serde
  (:require clojure.contrib.seq-utils)       ;needed for serde

  (:gen-class))
  
(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D)
        )  
		
(use 'calx)
(use 'clojure.contrib.math)

(set! *warn-on-reflection* true)


(defn serialize
  "Print a data structure to a file so that we may read it in later."
  [data-structure #^String filename]
  (clojure.contrib.duck-streams/with-out-writer
    (java.io.File. filename)
    (binding [*print-dup* true] (prn data-structure))))

;; This allows us to then read in the structure at a later time, like so:
(defn deserialize [filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))



  
(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))
  
  

(defn -main [& args]
  (println "Its not easy enough to get started with Clojure programming")
  (println (hello args))
)




(println "hello there from core.clj file")





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops18
;SAVE and LOAD from file, 
;TODO BIG onload onto GPU with clojure top level references
;DONE Introduced coerced, mutable datatype for large vector
;DONE loat the large dataset into the atom,  This is sustainable for ongoig loops
;;New best practice: provide 'default' values for veraibles with (def ...) at top level, rebind them with let within function definitons...
;;This way sub statements can be run at top level + debuged without having to recreate inner function state?? aaaHHaaaaaaaa, why doesn't the literature just say that? It's so much easier to develop with closures!


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
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)
;(def inputvec_float2 (vec (for [i (range (* 64 64 64 32))] (float (rand)))))
;testing if we can coerce the vector data type, use (float-array..) instead of vec.
(swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array (* 64 64 64 32)
                                   (for [i (range (* 64 64 64 32))] (float (rand))))))  ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
(println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

								   ;(def ^floats inputvec_float2 (float-array (* 64 64 64 32)
;     (for [i (range (* 64 64 64 32))] (float (rand)))))

;(def inputvec_float2 (vec (for [i (range global_clj_size)] (float (rand)))))

;(def inputvec_float3 (vec (for [i (range (* 64 64 64 32))] (rand))))

(quote
(time (swap! OpenCLoutputAtom4 (fn [foo] 
                               (^floats float-array (* 64 64 64 64 2)
                                   (for [i (range (* 64 64 64 64 2))] (float (rand)))))))
(time (swap! OpenCLoutputAtom4 (fn [foo] 
                               (^floats float-array (* 64 64 64 32)
                                   (for [i (range (* 64 64 64 32))] (float (rand)))))))
(* 64 64 64)
								   
(count @OpenCLoutputAtom3)

)


(quote
;"tring out beffer creation
(def clbuffer_a (with-cl (wrap inputvec_float :float32)))
(with-cl (create-buffer :float32 32))
(create-buffer-  :float32 32 :in-out)
(with-cl (with-program (compile-program sourceOpenCL2) @(enqueue-read clbuffer_a)))
(with-cl (with-program (compile-program sourceOpenCL2)
(def clbuffer_a (wrap inputvec_float :float32))))
(with-cl (with-program (compile-program sourceOpenCL2)
(enqueue-read clbuffer_a)))
(with-cl
(def clbuffer_b (mimic clbuffer_a)))
)


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
     ; inputvec_float (vec (for [i (range global_clj_size)] (float (rand))))
	  ]

;;(with-cl (def openCLbuffer (wrap (vec (for [i (range 10000000)] (rand))) :int32)))
;;(with-cl @(enqueue-read openCLbuffer ))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [;clbuffer_a (wrap @OpenCLoutputAtom2 :float32)
          ;clbuffer_b (mimic clbuffer_a)
          ;c (mimic clbuffer_a)
          ;d (mimic clbuffer_a)
		  ;e (mimic clbuffer_a)
		  ;f (mimic clbuffer_a)
		  ;g (mimic clbuffer_a)
		  ;h (mimic clbuffer_a)
          cl_localsize localsize]
		  
		;(acquire! clbuffer_a)
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
		(def clbuffer_b (mimic clbuffer_a))
		;(def clbuffer_c (mimic clbuffer_a))
		;(def clbuffer_d (mimic clbuffer_a))
		;(def clbuffer_e (mimic clbuffer_a))
		
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize)
				      ;(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_d cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_e cl_localsize)
                      (enqueue-barrier)
			          (finish))
               (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))			 
      ;(swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read clbuffer_a))))  ;we don't cate about old a, which was the atom value beforehand...
      (def startnanotime_bufferReadOutTime (. System (nanoTime)))
	  (swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
	  (def endnanotime_bufferReadOutTime (. System (nanoTime)))
	  ;(swap! OpenCLoutputAtom3 (fn [foo] (deref (enqueue-read clbuffer_c))))
      (release! clbuffer_a)
      (release! clbuffer_b)
	  ;(release! clbuffer_a)
    nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

									
									
(if	(= timingonly :timeingonly)
      (do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)
	   
	   (do
	   
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
	   
         (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
								
         (def startnanotime_clj (. System (nanoTime)))
        ; (def testOpenCl_vs_clj (reduce (fn [coll x]   ;Note can not do full test because original value is lost, may still keep copy of old during development to allow for testing for correct output out of openCL kernel
        ;   (conj coll (== (nth @OpenCLoutputAtom2 x) (* (nth @OpenCLoutputAtom1 x) (nth @OpenCLoutputAtom1 x)) )))
        ;      [] (range 0 global_clj_size)))
		(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))	  
			  
         (def endnanotime_clj (. System (nanoTime)))
         ;(println "testing output accuracy: " testOpenCl_vs_clj )

         (println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")
         ))

		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)

;(testoutputs (* 2 2 2) 2  1000 :timeingonly)
;(testoutputs (* 64 64 64 32) 1  3000 :timeingonly)
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)

;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))





