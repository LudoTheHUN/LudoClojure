

;; Added my Ludo to uberTest compatibility.
  (def sourceOpenCL
  "__kernel void square (
       __global const float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + a[gid];
  }")

(def answerIs
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
      (enqueue-read b)))))
	  
	  
;;Lets see what we can do here....
;build random float array...

(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [(rand) (rand) (rand) (rand)] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  
      (enqueue-read b)))))
	  
(deref answerIs2)
	  
(vec (repeat 4 (rand)))
(def RandomFloatVec (vec (for [i (range 100)] (rand))))

(def answerIs3
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap (vec (repeat 100 (rand))) :float32)
          b (mimic a)]
      (enqueue-kernel :square 100 a b)
      (enqueue-read b)))))


(def RandomFloatVec (vec (for [i (range 100)] (rand))))
(def answerIs4
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap RandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 100 a b)
      (enqueue-read b)
		 ))))
		 
(count @answerIs4)
(println (count @answerIs4))
;(release! b)


;;Ram Test...how big can we get?  This works but seems wrong as release! does not work... (can not run twise without running out of vRAM)
(def largelist (for [i (range 50000)] (rand)))
(def LRandomFloatVec (vec largelist))
(time (def answerIs5
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 50000 a b)
      
	  (def AnswerAtomizedBackToMainRam5 (atom @(enqueue-read b)))
	  (release! a)  ;;This does not work... what is a 'buffer'?
	  (release! b)
	  ;;(released? a)
	  )))))
(count @AnswerAtomizedBackToMainRam5)
(nth @AnswerAtomizedBackToMainRam5 1)


;;example6
(def largelist (for [i (range 25000)] (rand)))
(def LaRandomFloatVec (vec largelist))
(def AnswerAtomizedBackToMainRam6 (atom []))

(def AnswerAtomizedBackToMainRam6
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 25000 a b)
      (enqueue-read b)
	  ;(swap! AnswerAtomizedBackToMainRam6 (fn [_] @(enqueue-read b)))
	  ;(release! a)  ;;This does not work... what is a 'buffer'?
	  ;(release! b)
	  ;;(released? a)
	  ))))

(def LaRandomFloatVec (vec largelist))
	  
(println (count @AnswerAtomizedBackToMainRam6))
(println (nth LaRandomFloatVec 1))
(println "Answer from CPU: " (* (nth LaRandomFloatVec 1) (nth LaRandomFloatVec 1)))
(println "Answer from GPU: " (nth @AnswerAtomizedBackToMainRam6 1))
;(println (nth LaRandomFloatVec 2499999))
;(println "Answer from CPU Last: " (* (nth LaRandomFloatVec 2499999) (nth LaRandomFloatVec 2499999)))
;(println "Answer from GPU:Last: " (nth @AnswerAtomizedBackToMainRam6 2499999))



(defn AnswerAtomizedBackToMainRam7 []
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 2500000 a b)
      (enqueue-read b)
	  ))))
;;;NOTE looking at AnswerAtomizedBackToMainRam7 directly will TAKE ALL RAM and NOT RELEASE.
;;; @AnswerAtomizedBackToMainRam7 will require output is looked at, but will then allow GC to clean up ram..


(def largelist (for [i (range 2500000)] (rand)))
(def LaRandomFloatVec (vec largelist))	  
(println "Answer from CPU: " (* (nth LaRandomFloatVec 1) (nth LaRandomFloatVec 1)))
(println "Answer from GPU: " (nth @(AnswerAtomizedBackToMainRam7) 1))

(def HeldOutputEnqueueReadHolder7 (AnswerAtomizedBackToMainRam7))
(defn HeldOutputEnqueueReadHolder_read []
     (nth @HeldOutputEnqueueReadHolder7 1))
(HeldOutputEnqueueReadHolder_read)    ;Only Reading releases the RAM held by the (with-cl...(enqueue-read b)) block, becuase the enqueue-read 'stackes' the read queue up.,,,
	 
	 
(time (doseq [i (range 1 2)]  @(AnswerAtomizedBackToMainRam7)))





(def sourceOpenCL
  "__kernel void square (
       __global const float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] * a[gid];
  }")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  
;;;;; Iteration loops1
(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1 2 3 5] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
	  (enqueue-kernel :square 4 a b)
      (enqueue-read b)))))
	  
(deref answerIs2)
;;;;; Iteration loops2
(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.1 1.2 1.3 1.4] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
	  (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
      (enqueue-read b)))))
	  
(deref answerIs2)

;;;;; Iteration loops3
(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.0000000 1.00000000 1.0000000 1.00000000] :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a) (println "done:" x))   (range 1 10)))
      (enqueue-read b))))))
	  
(deref answerIs2)

;;;;; Iteration loops4
(quote 
(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.0000000 1.00000000 1.0000000 1.00000000] :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a))   (range 1 100000)))
      ;(enqueue-read b)
	  )))))
	  
(deref answerIs2))

;;;;; Iteration loops4
(quote 
(def largelist (for [i (range 25000)] 1.0))
(def LaRandomFloatVec (vec largelist))

(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 25000 b a))   (range 1 100000)))
      ;(enqueue-read b)
	  )))))
)
;;;;;;  26secs for 100000 *2 FLOPS over 25000 vector... or 192,000,000 operations per second...
(deref answerIs2)


;;;;; Iteration loops4
(quote 
(def largelist (for [i (range 500000)] 1.0))
(def LaRandomFloatVec (vec largelist))

(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (dorun 100 (fn [] (enqueue-kernel :square 5000000 a b) (enqueue-kernel :square 500000 b a)) )
      ;(enqueue-read b)
	  (release! a)
	  (release! b)
	  )))))
(count (deref answerIs2) )
	  
)
;;;;;;  26.0secs for 100000  *2 FLOPS over   25000 vector... or    192,000,000 operations per second...
;;;;;;   2.2sec  for     10  *2             500000           or     10,000,000 per second
;;;;;;  35.0sec      1000000 *2 FLOPS over  500000 vector    or  BOO2,850,000,000 per second
(deref answerIs2)

;(dorun (repeatedly 100 (fn [] (println "fpp")  )))

;;;;; Iteration loops5
(quote 
  (def sourceOpenCL
  "__kernel void square (
       __global float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + 1;
  }")

(def largelist (for [i (range 2000000)] 1))
(def LaRandomFloatVec (vec largelist))


(time (dorun  0 (repeatedly (fn []     ;;--massive costs of  openCL bootup, 160ms or so!
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      ;(dorun 200000 (repeatedly (fn [] (enqueue-kernel :square 200 a b) (enqueue-kernel :square 200 b a))  ))
	  ;(dorun 200000 (repeatedly (fn [] (enqueue-kernel :square 200 a b) (enqueue-kernel :square 200 b a))  ))    ;;Why does this eat ram????
	   (dorun 10 (repeatedly
	                          (fn [] (enqueue-kernel :square 2000000 a b) (enqueue-kernel :square 2000000 b a))  ))
	  ;;Trying loop recur...
	  
	  (enqueue-kernel :square 2000000 a b)
	  (release! a)
	  (release! b)
      (def answerIs2b (enqueue-read b))
	  (def answerIs2a (enqueue-read a))
	  ;(count (deref answerIs2b) )
	  nil
	  )))
))))

(count (deref answerIs2b) )
(* 2 (nth LaRandomFloatVec 1))
(nth (deref answerIs2b) 1)
(nth (deref answerIs2a) 1)

;;;; 2000 FLOPS * 2000000 items in 0.757 -> (/ (* 2000 2000000) 0.757) -> 5,284,000,000
;;;; 2000 FLOPS * 2000000 items in 0.757 -> (/ (* 2000 2000000) 0.757) -> 5,284,000,000


(def outputInCLJ_Ram (deref answerIs2))
(nth outputInCLJ_Ram  1)    ;Not that accessing the ref on the graphics card is SLOW
)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops6
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(def largelist (for [i (range 1000000)] 1))
(def LaRandomFloatVec (vec largelist))


(def myFooAtom1 (atom 0))
(def myFooAtom2 (atom 0))
(time 
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time (dorun 1000 (repeatedly(fn []
	           
						(wait-for (enqueue-kernel :square 1000000 a b))
						(wait-for (enqueue-kernel :square 1000000 b a))
						))))
	  (release! a)
	  (release! b)
	  (time (swap! myFooAtom2 (fn [x] (deref (enqueue-read b)))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil
	  )))
)


(count (deref myFooAtom2) )
(time (nth (deref myFooAtom2) 1))
(time (count (deref myFooAtom2)))
(time (reduce + (deref  myFooAtom2)))

(count (deref (deref myFooAtom2)))
(swap! myFooAtom2 (fn [x] 1))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops7
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(defn buildDatatoWorkWith [sizeOfarrayf]
 (def largelist (for [i (range sizeOfarrayf)] 1))
 (def LaRandomFloatVec (vec largelist))

 (def myFooAtom1 (atom 0))
 (def myFooAtom2 (atom 0)))

(defn RunOneOpenCL_uberloop [InnerLoopCount sizeOfarray] 
(println "starting")
(time 
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time (dorun InnerLoopCount (repeatedly(fn []
	           
						(wait-for (enqueue-kernel :square sizeOfarray a b))
						(wait-for (enqueue-kernel :square sizeOfarray b a))
						))))     ;have feeling this is stack overflowing... need to use loop recur?
	  (release! a)
	  (release! b)
	  ;(time (swap! myFooAtom2 (fn [x] (deref (enqueue-read b)))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil
	  )))
)
)  ;;RunOneOpenCL_uberloop
;(RunOneOpenCL_uberloop)

(defn SafeLoop [n functionToCall Isize asize]
  (loop [k n]
    (functionToCall Isize asize)
    (if (= k 1)
      (println "Done Work")
      (recur (dec k) ))))

(def InnerLoopCount 100)
(def sizeOfarray 1000000)
(buildDatatoWorkWith sizeOfarray)
(SafeLoop 50 RunOneOpenCL_uberloop InnerLoopCount sizeOfarray)

;Benchmarks
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 1.4))) 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 160))) 


(count LaRandomFloatVec)
(count (deref myFooAtom2) )
(time (nth (deref myFooAtom2) 1))
(time (count (deref myFooAtom2)))
(time (reduce + (deref  myFooAtom2)))

(count (deref (deref myFooAtom2)))
(swap! myFooAtom2 (fn [x] 1))
)





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops8
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(defn buildDatatoWorkWith [sizeOfarrayf]
 (def largelist (for [i (range sizeOfarrayf)] 1))
 (def LaRandomFloatVec (vec largelist))
 (def LaRandomFloatVecAtom (atom LaRandomFloatVec))
 (def myFooAtom1 (atom 0))
 (def myFooAtom2 (atom 0)))

 
 
(defn RunOneOpenCL_uberloop [InnerLoopCount sizeOfarray] 
 (println "starting")
 (time 
 (with-cl ;with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time   (loop [k InnerLoopCount]
						;(wait-for (enqueue-kernel :square sizeOfarray (wrap @LaRandomFloatVecAtom :int32) b))   ;;DEMO OF LOADING IN AN ARRAY FROM THE 'OUTSIDE', this could be in a (let []...) that would cover a whole block
						(wait-for (enqueue-kernel :square sizeOfarray a b))
						(enqueue-barrier)
						(wait-for (enqueue-kernel :square sizeOfarray b a))
						(enqueue-barrier)
						
                (if (= k 1)
				
                nil
                (recur (dec k) ))))     ;have feeling this is stack overflowing... need to use loop recur?
	  (release! a)
	  (release! b)	  
	  ;(swap! myFooAtom2 (fn [x] (enqueue-read b)))
	  ;;(time (swap! myFooAtom2 (fn [x] (enqueue-read b))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil)))))
;;RunOneOpenCL_uberloop
;(RunOneOpenCL_uberloop)

(defn SafeLoop [n functionToCall Isize asize]
  (loop [k n]
    (functionToCall Isize asize)
    (if (= k 1)
      (println "Done Work")
      (recur (dec k) ))))

(def InnerLoopCount 10)
(def sizeOfarray 1000000)
(buildDatatoWorkWith sizeOfarray)
(SafeLoop 10 RunOneOpenCL_uberloop InnerLoopCount sizeOfarray)

;Benchmarks
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 1.4))) 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 160) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 0.013) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi , 7.1B 10MArray 




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops8.1   Lazy onloading...
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(defn buildDatatoWorkWith [sizeOfarrayf]
 (def largelist (for [i (range sizeOfarrayf)] 1))
 (def LaRandomFloatVec (vec largelist))
 (def LaRandomFloatVecAtom (atom LaRandomFloatVec))
 (def myFooAtom1 (atom 0))
 (def myFooAtom2 (atom 0)))

 (def largelistLazyOnload (atom (vec (for [i (range 1000000)] 1) )))   ;; This defines a large lazy sequence
 ;;what if the input and the output are the same (atom) large vecotor
 
(defn RunOneOpenCL_uberloop [InnerLoopCount sizeOfarray] 
 (println "starting")
 (time 
 (with-cl ;with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap (deref largelistLazyOnload) :int32)   ;largelistLazyOnload can be a lazy sequence, however wrap will still hit ram... this has to be a vec
          b (mimic a)]
	  
	  (time   (loop [k InnerLoopCount]
						;(wait-for (enqueue-kernel :square sizeOfarray (wrap @LaRandomFloatVecAtom :int32) b))   ;;DEMO OF LOADING IN AN ARRAY FROM THE 'OUTSIDE', this could be in a (let []...) that would cover a whole block
						(wait-for (enqueue-kernel :square sizeOfarray a b))
						(enqueue-barrier)
						(wait-for (enqueue-kernel :square sizeOfarray b a))
						(enqueue-barrier)
                        ;(finish)
					    (release! a)
	                    (release! b)
						(finish)
                (if (= k 1)
				
                nil
                (recur (dec k) ))))     ;have feeling this is stack overflowing... need to use loop recur?
	  (release! a)
	  (release! b)	  
	  ;(swap! myFooAtom2 (fn [x] (enqueue-read b)))
	  (time (swap! largelistLazyOnload (fn [x] (deref (enqueue-read b)))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil)))))
;;RunOneOpenCL_uberloop
;(RunOneOpenCL_uberloop)

(defn SafeLoop [n functionToCall Isize asize]
  (loop [k n]
    (functionToCall Isize asize)
    (if (= k 1)
      (println "Done Work")
      (recur (dec k) ))))

(def InnerLoopCount 1000)
(def sizeOfarray 1000000)
;;(buildDatatoWorkWith sizeOfarray)
(SafeLoop 50 RunOneOpenCL_uberloop InnerLoopCount sizeOfarray)

;Benchmarks
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 1.4))) 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 160) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 0.013) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi , 7.1B 10MArray 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 0.721) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi , 7.1B 10MArray 



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops9
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 1.0;
    b[gid] = a[gid] + foo;
  }")


(def ArrayToPass (atom [12.0 34.0 45.0 67.0]))

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      ;(enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 (wrap @ArrayToPass :float32) b)
      (enqueue-read b))))
	  
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      ;(enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 a b)
      (enqueue-read b))))


	  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops10    -meny kernels?   : OK for many kernels as per this demo
(quote 
  
(def sourceOpenCL2
  "
  __kernel void square (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 1.0;
    b[gid] = a[gid] + foo;
  }

  __kernel void square2 (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 2.0;
    b[gid] = a[gid] * foo;
  }
  ")

(def ArrayToPass (atom [12.0 34.0 45.0 67.0]))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
	  (enqueue-kernel :square 4 (wrap @ArrayToPass :float32) b)
	  (enqueue-kernel :square 4 b a)
	  ;; sequential work makes the buffer persists, but barriers are likely needed for paralelisim safety. 
      (enqueue-read a))))
	    
)
		
		
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops11    -may buffers, postional access based on ints in another buffer
;;Difficult getting a single int out... maybe an array of floats denoting different outputs?  OK!!
;;NOTE: any mixing of datatypes can be silently catastrophic!
;;NOTE: this is very inneficent (positonvalout array outputs are computed 4 times for the same set of numbers...)
(quote 

(def sourceOpenCL2
  "
  __kernel void testingbuffers (
       __global float *a,
       __global float *b,
	   __global int *inta,
	   int positon,
	   __global int *positonvalout
	   ) {
    int gid = get_global_id(0);
	int lsize = get_local_size(0);  
	int lid = get_local_id(0);
	
	float foo = 1.0;
    b[gid] = a[gid] + foo;
	positonvalout[0] =  inta[positon];
	positonvalout[1]  =  inta[0] + inta[1] + 5;
	positonvalout[2]  = gid ;
	positonvalout[3]  = lsize ;
	positonvalout[4]  = lid;
  }
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11.5 12.4 5.0001] :float32)
          b (mimic a)
		  inta (wrap [8 7 3 4 7 3 1 2] :int32)
		  positon 0
		  positonvalout (wrap [0 0 0 0 0 0 0] :int32)
		  ]
      (enqueue-kernel :testingbuffers 4 a b inta positon positonvalout)
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read b))))
	  (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read positonvalout))))
	nil)))
@OpenCLoutputAtom1
@OpenCLoutputAtom2

)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops12    -CL like itteration within kernel, interkernel 'spin', 
;;Optimisation oportunity, global device memory read is 'expensive'.... (mac reseach CL episode6, 14:00+)
;;Notes towards mdh_opt.cl file - episde 6 source.
;;..so use shared memory... 16kb of shared memory per SM??
;;5 blocks *64 elements *floats size = 1280bytes...   (float is thus 4bytes)
;;1280bytes per work group.
;; lid is 0 to 63 inclusive.
;; top level itteration is in groupings of 64 (lsize)... as in for( iatom = 0; iatom < natoms; iatom+=lsize )
;;whole point is to two coelesed load from memory....
;;no guarantees as the what different threads have done already /yet. Hence neen a barier to get everyone on the same page
;;might not get a correct answer without the loc!
;;it's all about lots of global calls vs local calls which are faster?
;;bank conflict??
;;Hardware will need to context switch as some points ,
;; ...so the shared memory may start to beoverwriten by the second half warp, distroying what the slower(say) worp one still needs to be consistant (effectively the shared data will be out of sink over the top level loop)
;;will need to pad out to power of 2 sizes?
;;Seems this shared memory thing is high ovearhead? 
(quote 

(def sourceOpenCL2
  "
  __kernel void testingbuffers (
       __global float *a,
       __global float *b,
	   __global int *inta,
	   int positon,
	   __global int *positonvalout
	   ) {
    int gsize = get_global_size(0);
	int gid   = get_global_id(0);
	int lsize = get_local_size(0);  
	int lid   = get_local_id(0);  //FOO testing commenting
	
	float foo = 1.0;
    b[gid] = a[gid] + foo;
	positonvalout[gid ]  = inta[positon];
	positonvalout[gid + gsize]  = inta[0] + inta[1] + 5;
	positonvalout[gid + gsize * 2]  = gid ;
	positonvalout[gid + gsize * 3]  = lsize ;
	positonvalout[gid + gsize * 4]  = lid;
	positonvalout[gid + gsize * 5]  = gsize;
	///// barrier(CLK_LOCAL_MEM_FENCE);
  }
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))
(def OpenCLoutputAtom3 (atom 0))
(def OpenCLoutputAtom4 (atom 0))
(def OpenCLoutputAtom5 (atom 0))


(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11.5 12.4 5.0001] :float32)
          b (mimic a)
		  inta (wrap [8 7 3 4 7 3 1 2] :int32)
		  positon 1
		  positonvalout (wrap [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0] :int32)
		  globalsize 4
		  localsize 4
		  ]
	  (local 2)
      ;(enqueue-kernel :testingbuffers globalsize a b inta positon positonvalout)
	  (wait-for (enqueue-kernel :testingbuffers globalsize  a b inta positon positonvalout))
	  (enqueue-barrier)
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read a))))
	  (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read b))))
	  ;(swap! OpenCLoutputAtom3 (fn [x] (deref (enqueue-read inta))))
	  ;(swap! OpenCLoutputAtom4 (fn [x] (deref (enqueue-read positon))))
	  (swap! OpenCLoutputAtom5 (fn [x] (deref (enqueue-read positonvalout))))
	nil)))
@OpenCLoutputAtom1
@OpenCLoutputAtom2
@OpenCLoutputAtom5




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops13  local sizes...   ;set at 256 , no clear way to change... asked here: https://github.com/ztellman/calx/issues/3
(quote 

(def sourceOpenCL2
  "
  __kernel void testingbuffers (
       __global float *a,
       __global float *b,
	   __global int *inta,
	   int positon,
	   __global int *positonvalout,
	   __local int *local_memtemp
	   ) {
    int gsize = get_global_size(0);
	int gid   = get_global_id(0);
	int lsize = get_local_size(0);  
	int lid   = get_local_id(0);  //FOO testing commenting
	
	float foo = 1.0;
    b[gid] = a[gid] + foo;
	  positonvalout[gid]  = lid ;
	//positonvalout[gid ]  = inta[positon];
	//positonvalout[gid + gsize]  = inta[0] + inta[1] + 5;
	//positonvalout[gid + gsize * 2]  = gid ;
	//positonvalout[gid + gsize * 3]  = lsize ;
	//positonvalout[gid + gsize * 4]  = lid;
	//positonvalout[gid + gsize * 5]  = gsize;
	////// barrier(CLK_LOCAL_MEM_FENCE);
  }
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))
(def OpenCLoutputAtom3 (atom 0))
(def OpenCLoutputAtom4 (atom 0))
(def OpenCLoutputAtom5 (atom 0))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap (vec (for [i (range 1028)] (rand))) :float32)
          b (mimic a)
		  inta (wrap (vec (for [i (range 1028)] 1)) :int32)
		  positon 1
		  positonvalout (mimic inta)
		  globalsize 1028
		  ]
      ;(enqueue-kernel :testingbuffers globalsize a b inta positon positonvalout)
	  (wait-for (enqueue-kernel :testingbuffers globalsize a b inta positon positonvalout (local 64)))
	  (enqueue-barrier)
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read a))))
	  (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read b))))
	  ;(swap! OpenCLoutputAtom3 (fn [x] (deref (enqueue-read inta))))
	  ;(swap! OpenCLoutputAtom4 (fn [x] (deref (enqueue-read positon))))
	  (swap! OpenCLoutputAtom5 (fn [x] (deref (enqueue-read positonvalout))))
	nil)))
@OpenCLoutputAtom1
@OpenCLoutputAtom2
@OpenCLoutputAtom5


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops13.1  local sizes...   ;set at 256 , no clear way to change... asked here: https://github.com/ztellman/calx/issues/3
(quote 

(def sourceOpenCL2
  " 
__kernel void square(
    __global float *input,
    __global float *output,
    const unsigned int count)
{
    int i = get_global_id(0);
    if (i < count)
        output[i] = input[i] * input[i];
}
  
__kernel void squarelocal(
    __global float *input,
    __global float *output,
    __local float *temp,
    const unsigned int count)
{
    int gtid = get_global_id(0);
    int ltid = get_local_id(0);
    if (gtid < count)
    {
        temp[ltid] = input[gtid];
        output[gtid] =  temp[ltid] * temp[ltid];
    }
}
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))
(def OpenCLoutputAtom3 (atom 0))
(def OpenCLoutputAtom4 (atom 0))
(def OpenCLoutputAtom5 (atom 0))

(defn testoutputs [x]
(let [global_clj_size x
      inputvec (vec (for [i (range global_clj_size)] (rand)))]

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap inputvec :float32)
          b (mimic a)
          c (wrap inputvec :float32)
          d (mimic a)
          globalsize global_clj_size
          ]
      (time (enqueue-kernel :square      globalsize a b                         globalsize))
      (time (enqueue-kernel :squarelocal globalsize c d (local global_clj_size) globalsize))
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read a))))
      (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read b))))
      (swap! OpenCLoutputAtom3 (fn [x] (deref (enqueue-read c))))
      (swap! OpenCLoutputAtom4 (fn [x] (deref (enqueue-read d))))
      (release! a)
      (release! b)
      (release! c)
      (release! d)
    nil)))
      
(println 
(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (@OpenCLoutputAtom4 x) )))
 [] (range 0 global_clj_size))   ;tests outputs buffers were the same
 
(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom3 x) )))
 [] (range 0 global_clj_size))   ;tests input bufferes were the same

(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (* (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom1 x)) )))
 [] (range 0 global_clj_size))    ;tests the :square computation came back with correct answer

 (reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom4 x) (* (@OpenCLoutputAtom3 x) (@OpenCLoutputAtom3 x)) )))
 [] (range 0 global_clj_size))    ;tests the :squarelocal computation came back with correct answer
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(count @OpenCLoutputAtom3)
(count @OpenCLoutputAtom4)
 )))
 
(testoutputs (* 2))
(testoutputs (* 2 2))
(testoutputs (* 2 2 2))
(testoutputs (* 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2 2 2 2 2))
(testoutputs (* 2 2 2 2 2 2 2 2 2 2 2 2 2))  ;; 8192*sizeoffloat(4)= 32768bites in local memory = This is the largest things can be for the local call.. on my GPU card.
(testoutputs (* 2 2 2 2 2 2 2 2 2 2 2 2 2 2))  ;; this and any larger local sizes fail with:
;java.lang.RuntimeException: Exception while waiting for events [Event {commandType: ReadBuffer}] (NO_SOURCE_FILE:0)


(count @OpenCLoutputAtom4)
 
(@OpenCLoutputAtom1 0)
(@OpenCLoutputAtom4 0)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops14
;LOCAL LOOPS WITHIN KERNEL

(quote 

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
									
(println "testing output: " (time (reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (* (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom1 x)) )))
 [] (range 0 global_clj_size))))
(println "Total nanotime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
))




(testoutputs (expt 2 11) 1  1000)

(testoutputs (expt 2 23) 1  1000)  ;7.7
(testoutputs (expt 2 22) 1  10)  ;6.51 GFLOPS
(testoutputs (expt 2 21) 2  1000)  ;4.5
(testoutputs (expt 2 10) 4  1000)  ;2.59

(testoutputs (expt 2 19) 4  10000)
(testoutputs (expt 2 20) 2  1)
(testoutputs (expt 2 21) 1  1)
(testoutputs 1 (expt 2 21)  1)
(testoutputs (expt 2 21) 1  1)
(testoutputs 1 (expt 2 22)  1)
 
 )
 
 
 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops15
;Better closures!
;;New best practice: provide 'default' values for veraibles with (def ...) at top level, rebind them with let within function definitons...
;;This way sub statements can be run at top level + debuged without having to recreate inner function state?? aaaHHaaaaaaaa, why doesn't the literature just say that? It's so much easier to develop with closures!


(quote 

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
  
(def OpenCLoutputAtom1 (atom [0 1 2 3 4]))
(def OpenCLoutputAtom2 (atom [1 1 2 3 4]))
(def OpenCLoutputAtom3 (atom [2 1 2 3 4]))
(def OpenCLoutputAtom4 (atom [3 1 2 3 4]))
(def OpenCLoutputAtom5 (atom [4 1 2 3 4]))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (rand))))

(defn testoutputs [global local InnerLoopCount]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
	  InnerLoopCount InnerLoopCount
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




(testoutputs (expt 2 11) 1  1000)

(testoutputs (expt 2 23) 1  1000)  ;7.7
(testoutputs (expt 2 22) 1  10)  ;6.51 GFLOPS
(testoutputs (expt 2 21) 2  1000)  ;4.5
(testoutputs (expt 2 10) 4  1000)  ;2.59

(testoutputs (expt 2 19) 4  10000)
(testoutputs (expt 2 20) 2  1)
(testoutputs (expt 2 21) 1  1)
(testoutputs 1 (expt 2 21)  1)
(testoutputs (expt 2 21) 1  1)
(testoutputs 1 (expt 2 22)  1)
 
 )

 
 
 
 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops16
;onload onto GPU
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
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))
(def OpenCLoutputAtom3 (atom 0))
(def OpenCLoutputAtom4 (atom 0))
(def OpenCLoutputAtom5 (atom 0))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (rand))))

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
          c (mimic a)
          d (mimic a)
		  e (mimic a)
		  f (mimic a)
		  g (mimic a)
		  h (mimic a)
          cl_localsize localsize]
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize a b cl_localsize)
				      (enqueue-kernel :looper globalsize a c cl_localsize)
					  (enqueue-kernel :looper globalsize a d cl_localsize)
					  (enqueue-kernel :looper globalsize a e cl_localsize)
					  (enqueue-kernel :looper globalsize a f cl_localsize)
					  (enqueue-kernel :looper globalsize a g cl_localsize)
					  (enqueue-kernel :looper globalsize a h cl_localsize)  ;This seems a very dirty way to onload data into the GPU... then again... why not...
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

									
									
									
(println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")

									
(def startnanotime_clj (. System (nanoTime)))
(def testOpenCl_vs_clj(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (* (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom1 x)) )))
 [] (range 0 global_clj_size)))
(def endnanotime_clj (. System (nanoTime)))
(println "testing output: " testOpenCl_vs_clj )

(println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")


		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 16) 32  1000)
)



 
;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);




;(dorun 10 (repeatedly (fn [] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 50 b a))  ))
;(dorun 5 (repeatedly #(println "hi")))
;This is bad, takes up stack space!!
;(dorun (map (fn [] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a) (range 1 10))))
;(dorun (map #(println "hi" %) ["mum" "dad" "sister"])).

;TODO see how releaseing b will behave
;Add Calx to LudoClojure as a dependency .... more above exploratory code into LudoClojure
;;Getting problems with RAM utilisation

;1M  1600ms (when contended), but this effectively involves copying from normal ram to remote...
;10M 550ms
;;Started to run out of vRAM need to release the buffer with (release! d)?

;;http://www.overclock.net/nvidia-drivers-overclocking-software/256164-rivatuner-vram-usage.html
;;Monitoring GPU durig test runs... (especialy vRAM usage)

(defn -main [& args]
  (println "the following output will be out of openCL"  @answerIs))
  
;(defn main [& args]
;  (println "whats with this error huh?"  @answerIs))
  
;;;
