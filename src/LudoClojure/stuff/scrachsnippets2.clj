
(ns LudoClojure.core
  (:require clojure.contrib.seq-utils)
  (:require clojure.contrib.probabilities.random-numbers)  ;;gives lcg random stream generator
  (:require clojure.contrib.duck-streams )
  (:gen-class))

;Java interop examples
(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D)
        )

;Function for grabbing the screen
(defn GrabScreen [x y xd yd]
       (.createScreenCapture (Robot.)
                 (Rectangle. x y xd yd)))
(defn GrabScreenColorJavaArray [x y xd yd]
       (. (GrabScreen x y xd yd) getRGB 0 0 (- xd x) (- yd y) nil 0 (- xd x)))
;(GranScreenColorJavaArray 0 0 100 100)   ;Note: could be made simpler by just 
;pullig squares areas always
;(GranScreenColorJavaArray 1 1 7 9)
(defn SaveBufferedImage [x y xd yd]
       (ImageIO/write (.createScreenCapture (Robot.)
                 (Rectangle. x y xd yd))
               "JPG"
               (File. "/home/ludo/Documents/cljoutfile.jpg")))


(defn myCLJarrayFromJavaArrayFun [JavaIntArray]
      (let [JavaIntArray_Length (alength JavaIntArray)]
      (loop [lengthdone 0 OutputClosureArray [] ]
         (if (= lengthdone JavaIntArray_Length)
            ;(do (println (alength JavaIntArray)) (println JavaIntArray_Length " foos " lengthdone "fiis" OutputClosureArray)  OutputClosureArray)
              OutputClosureArray
           (recur 
             (inc lengthdone)  
             ;(do (println (aget JavaIntArray lengthdone) " lendone " lengthdone) (conj OutputClosureArray (aget JavaIntArray lengthdone)  ))
             (conj OutputClosureArray (aget JavaIntArray lengthdone))
           )))))
           
;(myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  10 12 14 15))
;(time (println (.length (myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  0 0 50 50)))))
         
(defn loopsomethingntimes [somethingtobedone ntimes]
        (loop [n 0]
           (if (= n ntimes)
           (println "done")
           (do (eval somethingtobedone)
               (recur (inc n))))))

(def x 100)
(def y 100)
(def x 50)
(def y 50)

(SaveBufferedImage 100 50 x y)
(myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  0 0 x y))

(loopsomethingntimes 
     '(time (do
              (let [ImageArray (myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  0 0 x y))]
              (println (.length ImageArray)))))
     10)


;;TODO Get a visualisation of what's in a clj colour array... look at setRGB 
;;TODO Look at ants example how to do things correctly....





;;TODO Figure out concurency so we can push jobs (eg: image grabing) to the background on a thread...
   

(.size (distinct boo))
(.size (distinct boo2))
(type boo)
(type boo2)




















(def factorial
  (fn [n]
    (loop [cnt n acc 1]
       (if (zero? cnt)
            acc
          (recur (dec cnt) (* acc cnt))))))
(factorial 10)  


(def testarray [12 234 65 :abc :abd])


           
(def pixColourCLJArray (myCLJarrayFromJavaArrayFun pixColourJavaArray))


;Test to see if these are the same
(alength pixColourJavaArray)  ;Lenth of the Java array  ... this is really just a pointer to memory...
(.length pixColourCLJArray)   ;Length of the CLJ array  ... a real closure array..




pixColourCLJArray
(type pixColourCLJArray)
(pixColourCLJArray 1)
(pixColourCLJArray 99)
(pixColourCLJArray 100)
(.length pixColourCLJArray)



      
(. clojure.lang.Num (from (alength pixColourJavaArray)))
(alength pixColourJavaArray)
(aget pixColourJavaArray 1)


(type
((conj [] (aget pixColourJavaArray 1)) 0)
)
(type 1)

(aget pixColourJavaArray 6)


(testarray 1)
(pixColourJavaArray 1)

(aget testarray 1)
(aget pixColourJavaArray 1)


(pixColourJavaArray 1)




(GrabScreen 10 20 30 40)









(. (. System (getProperties)) (get "os.name"))

(comment  show the Nano Time)
(. System (nanoTime))


;comment http://download.oracle.com/javase/1.4.2/docs/api/java/awt/Robot.htmls

(.mouseMove (java.awt.Robot.) 100 100)

gets the color oject for a pixel
(.getPixelColor (java.awt.Robot.) 100 100)

(.getPixelColor (java.awt.Robot.) 100 100)

(.getRed (.getPixelColor (java.awt.Robot.) 100 100))
(.getBlue (.getPixelColor (java.awt.Robot.) 100 100))
(.getGreen (.getPixelColor (java.awt.Robot.) 100 100))



;why fail?
;http://www.gettingclojure.com/cookbook:system
;This needs the inports as above
(.createScreenCapture (Robot.)
  (Rectangle. (.getScreenSize (Toolkit/getDefaultToolkit))))


(.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40))

;Write buffered image to file
(ImageIO/write (.createScreenCapture (Robot.)
                 (Rectangle. 5 5 100 400))
               "JPG"
               (File. "/home/ludo/Documents/cljoutfile.jpg"))








;Grab a screen capture object (buffered image)
(.createScreenCapture (Robot.)
                 (Rectangle. 10 20 20 40)) 

;Function that grabs a screen area
(defn GrabScreen [x y xd yd]
       (.createScreenCapture (Robot.)
                 (Rectangle. x y xd yd)))


(GrabScreen 10 20 30 40)  ;Returns a buffered image object


(. (GrabScreen 10 20 30 40) getRGB 4 5)   ; grabs an RGB value based on the pixel
;(. instance instanceMember args*)
(. (GrabScreen 10 20 16 17) getWidth)
(. (GrabScreen 10 20 16 17) getHeight)
(. (GrabScreen 10 20 16 17) toString)

(. (GrabScreen 10 20 30 40) getColorModel)

;Create a graphixs object based on the buffered image
(. (GrabScreen 10 20 30 40) createGraphics)
;fail
(.getBlue (do (. (GrabScreen 10 20 30 40) getColorModel))








;RGB array from buffered image
(GrabScreen 0 100 0 100)
;get a pixel values from the buffered image
(. (GrabScreen 1 1 100 100) getRGB 10 20 )
;what em I supposed to do with that???

;Colour value at a specific place
(. (GrabScreen 1 1 100 100) getRGB 10 20 )





;;Java array of ints??
;Length of array
(alength
    (. (GrabScreen 0 0 100 100) getRGB 0 0 100 100 nil 0 100))
(aget
    (. (GrabScreen 0 0 100 100) getRGB 0 0 100 100 nil 0 100) 10)
;array of 10000 values representing the odd rgb colours
(. (GrabScreen 0 0 100 100) getRGB 0 0 100 100 nil 0 100)
;how long does it take to grab these pixels.... 100ms for 600^2 area
(time (alength (. (GrabScreen 0 0 600 600) getRGB 0 0 600 600 nil 0 600)))





(time (def pixColourJavaArray (. (GrabScreen 0 0 100 100) getRGB 0 0 100 100 nil 0 100)))
(def pixColourJavaArray (. (GrabScreen 0 0 100 100) getRGB 0 0 100 100 nil 0 100))
;pixColourJavaArray this is already a clojure seq (sequence)
(aget pixColourJavaArray 1)
(first pixColourJavaArray)  ; first entry in the array
(alength axColourJavaArray)  ; length of the array
(frequencies pixColourJavaArray) ;frequcny of seach colour
;
;sequence of distinct colour values
(first (distinct pixColourJavaArray))

(defn clPixColourArray  [axColourJavaArray]
      (let [a (alength axColourJavaArray)]
        a)  ;retunr the a
        )
      
(clPixColourArray pixColourJavaArray)

(frequencies pixColourJavaArray)
;;removed in clojure 1.2 (includes? pixColourJavaArray 2642049)
(indexed pixColourJavaArray)

(aget pixColourJavaArray 11)
(cons "fooo" (frequencies pixColourJavaArray))
(first (frequencies pixColourJavaArray))





(sort pixColourJavaArray)
(sort (frequencies (sort pixColourJavaArray)))
(take 10 pixColourJavaArray)
(take 10 (frequencies pixColourJavaArray))
(take 10 (frequencies (take 300 pixColourJavaArray)))
(take-while (fn [x] (< x 1000)) pixColourJavaArray)



(take-while (partial > 10) (iterate inc 0))
((partial + 10) 10)  ;partial returns a function that takes the f(fist arg) and ads the argumeten up to the call....
(take-while (partial > 10) (iterate inc 0))
(reduce + (take-while (partial > 10) (iterate inc 0)))
(take-while (partial > -9612400) pixColourJavaArray)
(reduce + (take-while (partial > -9612400) pixColourJavaArray))

(iterate inc 0)  ; itterares from zero forever....



(first (frequencies (take 300 pixColourJavaArray)))
(rest (frequencies (take 300 pixColourJavaArray)))

(sequence? (rest (frequencies (take 300 pixColourJavaArray))))








;Fail
(. (. (GrabScreen 100 100 200 200) getRGB 99 99 ) getBlue)


(def ascreenGrab (GrabScreen 10 20 30 40))


(. ascreenGrab getRGB 4 5)



(. (GrabScreen 100 100 200 200) getRGB 45 45)



;the FAIL bin
(. (GrabScreen 10 20 30 40) getRGB 4 5 7 9)
(TYPE_INT_ARGB  (. (GrabScreen 10 20 30 40) getRGB 4 5))
(.getPixelColor (GrabScreen 10 20 30 40) 5 9)
(. (GrabScreen 1 1 100 100) getBlue 10 21 )




http://download.oracle.com/javase/1.4.2/docs/api/java/awt/Image.html
Buffered image, vs SunGraphics2D... one lets you manipulate the graphics others let you draw them...
;FAIL 
(.drawImage (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40)) 10 10 100 200)
(.createGraphics (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40)))
;....WIP
(.draw (.createGraphics (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40))))
(draw (Rectangle. 10 20 20 40))
;....

;local veriable
(let [a 10] (println a))


