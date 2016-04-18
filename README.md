# cluby -- Ruby Object Model

A Clojure library designed to ... well, that part is up to you.

![](https://clojars.org/org.clojars.sa1amander/cluby/latest-version.svg)

## install
```js
:dependencies [org.clojars.sa1amander/cluby "0.1.0-SNAPSHOT"]
```

## Usage

### 新建
从OBJECT, 和MODULE, 对象类中新建

```clojure
;; clojure
(def obj (OBJECT :allocate))
(def module (MODULE :allocate))
```

```ruby
# ruby
obj = Object.allocate
mod = Module.allocate
```

### 定义类实例方法
定义类，和类的实例方法instance-methods

```clojure
;; clojure
(defclass MyClass
    (defm method1 ([] :m))
    (defm multimethod (
        ([a] :m1)
        ([] :m0))))

(defm MyClass classMethod ([] :MyClass-method))

(def my-obj (MyClass :new))

(my-obj :method1) ;=> :m
(my-obj :multimethod 1) ;=> :m1
(my-obj :multimethod) ;=> :m0
```

```ruby
# ruby
class MyClass
    def method1()
        :dd
    end
end

def MyClass.classMethod(); :MyClass-method; end
```

### 定义类单件方法

```clojure
;; clojure
(defm MyClass a ([]
    :MyClass-method))
(MyClass :a) ;=> :MyClass-method
```

```ruby
# ruby
def MyClass.a()
    :MyClass-method
end
MyClass.a #=> :MyClass-method
```
...

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
