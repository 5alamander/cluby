(ns cluby.core-test
  (:require [clojure.test :as t])
  (:use [cluby.core]))

;;; ---- basic object functions ----

(def obj (OBJECT :allocate))
(def module (MODULE :allocate))

(t/deftest basic
  (t/testing "exist"
    (t/is OBJECT CLASS))
  (t/testing "OBJECT BASIC-OBJECT CLASS MODULE"
    (t/is (and
           (= (OBJECT :class) CLASS)    ; OBJECT
           (= (OBJECT :superclass) BASIC-OBJECT)
           (= (OBJECT :name) "OBJECT")
           (= (CLASS :class) CLASS)     ; CLASS
           (= (CLASS :superclass) MODULE)
           (= (CLASS :name) "CLASS")
           (= (MODULE :class) CLASS)    ; MODULE
           (= (MODULE :superclass) OBJECT)
           (= (MODULE :name) "MODULE"))
          ))
  (t/testing "OBJECT allocate"
    (t/is (and
           (= (obj :class) OBJECT)
           (= (module :class) MODULE)
           (= ((module :class) :superclass) OBJECT)))))

;;; ---- define class with class instance methods ----

(defclass MyClass
  (defm method1 ([] 'dd))
  (defm multimethod
    (([a] 'm2)
     ([] 'df)))
  )

(defm MyClass method ([] 'MyClass-method))

(def my-obj (MyClass :new))

(t/deftest test-class
  (t/testing "defm class instance method"
    (t/is (my-obj :to-s))
    (t/is (= (my-obj :multimethod 1) 'm2))
    (t/is (= (my-obj :multimethod) 'df))
    (t/is (= (MyClass :method) 'MyClass-method))))

;;; ---- extends with base-class ----

(defclass SubClass (extends MyClass)
  (defm method1 ([] ['wrap (self :super :method1)])))

;;; show function for SubClass
(defm SubClass show ([] :sub-class-function))

(def my-sub (SubClass :new))

(defclass Sub1Class (extends SubClass)
  (defm initialize
    (([] (self :set! :t 1))
     ([a] (self :set! :t a))))
  (defm method1 ([] ['sub1 (self :super :method1)]))
  (defm method-missing ([] :method-missing)))

(def my-sub10 (Sub1Class :new))
(def my-sub11 (Sub1Class :new 10))

(defm my-sub11 t ([] :#t))              ; def in eigenclass of my-sub11

(t/deftest test-extends
  (t/testing "extends class"
    (t/is (= (my-sub :multimethod 1) 'm2)))
  (t/testing "super keyword with 1 deeps"
    (t/is (= (my-sub :method1) ['wrap 'dd])))
  (t/testing "super keyword with 2 deeps"
    (t/is (= (my-sub10 :method1) ['sub1 ['wrap 'dd]])))
  (t/testing "test instance value, defined by initialize function"
    (t/is (= (my-sub10 :get :t) 1))
    (t/is (= (my-sub11 :get :t) 10)))
  (t/testing "eigenclass function for object"
    (t/is (and (= (my-sub11 :t) :#t)
               (= (my-sub10 :t) :method-missing))))
  (t/testing "eigenclass function for Class"
    (t/is (and (= (SubClass :show) :sub-class-function)
               (= (Sub1Class :show) :sub-class-function)
               (= (Sub1Class :method) 'MyClass-method)))))

;;; deep inherant
(defclass Step0
  (defm a ([] :step0_a)))

(defclass Step1 (extends Step0)
  (defm a ([] :step1_a))
  (defm b ([] (self :super :a))))

(defclass Step2 (extends Step1))

(defclass Step3 (extends Step2))

(t/deftest deep-extends
  (t/testing "call super in deep base class"
    (t/is (= ((Step3 :new) :b) :step0_a))))


;;; test module

(defmodule Amodule
  (defm a ([] :module-a))
  (defm b ([] :module-b)))

(defmodule Amodule2
  (defm a ([] :module2-a))
  (defm c ([a] [:module2-c :return a])))

(defm Amodule2 a ([] :eigen-method))

(defclass Empty
  (include Amodule)
  (include Amodule2))

(defclass Empty1 (extends Empty))

(def em (Empty :new))

(def em1 (Empty1 :new))

(def m1 (MyClass :new))
(def m2 (MyClass :new))

(m1 :extend Amodule)

(t/deftest module-test
  (t/testing "module-test"
    (t/is (= (em :a) :module2-a))
    (t/is (= (em :b) :module-b)))
  (t/testing "in sub class"
    (t/is (= (em1 :b) :module-b)))
  (t/testing "in object"
    (t/is (= (m1 :a) :module-a)))
  (t/testing "module eigen"
    (t/is (= (Amodule2 :a) :eigen-method))))

;;; test more delegate extensions
(def delegate0 (partial em1 :b))
(def delegate1 (partial em1 :c))

(t/deftest cluby-delegate
  (t/testing "cluby create delegate with partial"
    (t/is (and (= (delegate0) :module-b)
               (= (delegate1 :arg) [:module2-c :return :arg])))))
