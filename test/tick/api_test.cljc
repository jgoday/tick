;; Copyright © 2016-2017, JUXT LTD.

(ns tick.api-test
  (:require
    [clojure.test
     :refer [deftest is testing run-tests]
     :refer-macros [deftest is testing run-tests]]
    [tick.core :as t]
    [tick.protocols :as p]
    [tick.locale-en-us]
    [cljc.java-time.clock]
    [cljc.java-time.instant]
    [cljc.java-time.day-of-week]
    [cljc.java-time.month]
    [cljc.java-time.year]))

;; Constructor test

(deftest constructor-test
  (is (t/year? (t/year 2017)))
  (is (= 2017 (cljc.java-time.year/get-value (t/year 2017))))
  (is (t/month? (t/month 12)))
  (is (= t/DECEMBER (t/month 12)))
  (is (= (t/new-date 3030 3 3)
         (t/date "3030-03-03")))
  (is (-> (t/new-duration 1000 :millis)
          (t/inst)
          (t/instant)
          (cljc.java-time.instant/to-epoch-milli)
          (= 1000)))
  (is (= (t/new-year-month 2020 7)
         (t/year-month  "2020-07"))))

(deftest extraction-test
  (is (= 2 (t/int t/FEBRUARY)))
  (is (= 2 (t/int t/TUESDAY)))
  (is (= t/AUGUST (t/month (t/date-time "2017-08-08T12:00:00"))))
  (is (= t/AUGUST (t/month (t/year-month "2017-08"))))
  (is (= (t/year 2019) (t/year (t/zoned-date-time "2019-09-05T00:00:00+02:00[Europe/Oslo]"))))
  (is (= (t/year 2019) (t/year (t/offset-date-time "2019-09-05T00:00:00-03:00"))))
  (is (= (t/zone-offset "-04:00")
         (t/zone-offset (t/zoned-date-time "2019-03-15T15:00-04:00[America/New_York]"))))
  (is (= (t/zone-offset "-04:00")
         (t/zone-offset (t/offset-date-time "2019-03-15T15:00-04:00")))))

;; Point-in-time tests
(deftest today-test
  (t/with-clock (cljc.java-time.clock/fixed (t/instant "2017-08-08T12:00:00Z") t/UTC)
    (is (= (t/instant "2017-08-08T12:00:00Z") (t/now)))
    (is (= (t/date "2017-08-08") (t/today)))
    (is (= (t/date "2017-08-07") (t/yesterday)))
    (is (= (t/date "2017-08-09") (t/tomorrow)))
    (is (= 8 (t/day-of-month (t/today))))
    (is (= 2017 (t/int (t/year))))
    (is (= (t/date-time "2017-08-08T12:00:00") (t/noon (t/today))))
    (is (= (t/date-time "2017-08-08T00:00:00") (t/midnight (t/today))))))

(deftest instant-test
  (testing "instant basics"
    (is (t/instant? (t/instant (t/now))))
    (is (t/instant? (t/instant (str cljc.java-time.instant/min))))
    (is (t/instant? (t/instant (t/zoned-date-time)))))

  (deftest offset-date-time-test
    (let [t "2018-09-24T18:57:08.996+01:00"]
      (testing "offset date time basics"
        (is (t/offset-date-time? (p/parse t)))
        (is (t/offset-date-time? (t/offset-date-time (t/now))))
        (is (t/offset-date-time? (t/offset-date-time t)))
        (is (t/offset-date-time? (t/offset-date-time (t/date-time))))
        (is (t/offset-date-time? (t/offset-date-time (t/zoned-date-time))))))))

(deftest zoned-date-time-test
  (is (t/zoned-date-time? (p/parse "2020-12-15T12:00:10Z[Europe/London]")))
  (is (t/zoned-date-time? (p/parse "2020-12-15T12:00:10+04:00[Europe/London]"))))

(deftest fields-test
  (let [xs [(t/now)
            (t/zoned-date-time)
            (t/offset-date-time)
            (t/date-time)
            (t/date)
            (t/time)
            (t/year)
            (t/year-month)]]
    (doseq [x xs]
      (let [fields (t/fields x)
            fields-map (into {} fields)]
        (is (not-empty fields-map))
        (doseq [[f v] fields-map]
          (is (= v (get fields f)))
          (is (= :foo (get fields :bar :foo))))))))

(deftest formatting-test
  (testing "all predefined formatters exist"
    (doseq [pre-defined (vals t/predefined-formatters)]
      (is pre-defined)))
  (let [d "3030-05-03"]
    (is (= d (t/format :iso-local-date (p/parse d))))
    (is (= d (t/format (t/formatter :iso-local-date) (p/parse d))))
    (is (= d (t/format (t/formatter "YYYY-MM-dd") (p/parse d))))
    #?(:clj
       (is (= "3030-mai-03" (t/format (t/formatter "YYYY-MMM-dd" java.util.Locale/FRENCH) (p/parse d)))))))

(deftest epoch-test
  (is (= (cljc.java-time.instant/parse "1970-01-01T00:00:00Z") (t/epoch))))

;; Period arithmetic

(deftest addition-test
  (is (= (t/new-duration 5 :seconds) (t/+ (t/new-duration 2 :seconds) (t/new-duration 3 :seconds))))
  (is (= (t/new-duration 2 :minutes) (t/+ (t/new-duration 90 :seconds) (t/new-duration 30 :seconds)))))

(deftest subtraction-test
  (is (= (t/new-duration 3 :seconds) (t/- (t/new-duration 5 :seconds) (t/new-duration 2 :seconds)))))

;; Between test
(deftest between-test
  (is
    (=
      (let [now (t/now)]
        (t/between
          (t/<< now (t/new-duration 10 :seconds))
          (t/>> now (t/new-duration 10 :seconds))))
      (t/new-duration 20 :seconds)))
  (is
    (= (t/new-duration 48 :hours)
      (t/between (t/beginning (t/today)) (t/end (t/tomorrow)))))
  (is
    (=
      (t/new-duration 2 :minutes)
      (t/between "2020-01-01T12:00" "2020-01-01T12:02")))
  (is
    (=
      (t/new-duration 30 :minutes)
      (t/between (t/new-time 11 0 0) (t/new-time 11 30 0))))
  (is
   (=
    (t/new-duration 2 :minutes)
    (t/between #inst "2020-01-01T12:00" #inst "2020-01-01T12:02")))

  (testing "LocalDate"
    (is (= (t/new-period 1 :days)
          (t/between (t/date "2020-01-01")
            (t/date "2020-01-02"))))))

;; Range test

(deftest range-test
  (is (t/midnight? (t/beginning (t/today))))
  (is (t/midnight? (t/end (t/today))))
  (is (t/midnight? (t/beginning (t/year))))
  (is (t/midnight? (t/end (t/year)))))

;; Units test

(deftest units-test
  (is (=
        {:seconds 0, :nanos 1}
        (t/units (t/new-duration 1 :nanos))))
  (is
    (=
      {:years 10, :months 0, :days 0}
      (t/units (t/new-period 10 :years)))))

;; Comparison test

(deftest comparison-test
  (is
    (t/<
      (t/now)
      (t/>> (t/now) (t/new-duration 10 :seconds))
      (t/>> (t/now) (t/new-duration 20 :seconds))))
  (is
    (t/>
      (t/>> (t/now) (t/new-duration 20 :seconds))
      (t/>> (t/now) (t/new-duration 10 :seconds))
      (t/now)))
  (is (not
        (t/<
          (t/now)
          (t/>> (t/now) (t/new-duration 20 :seconds))
          (t/>> (t/now) (t/new-duration 10 :seconds)))))
  (let [at (t/now)]
    (is (t/<= at at (t/>> at (t/new-duration 1 :seconds))))
    (is (t/>= at at (t/<< at (t/new-duration 10 :seconds)))))

  (testing "durations"
    (is (t/> (t/new-duration 20 :seconds) (t/new-duration 10 :seconds)))
    (is (t/>= (t/new-duration 20 :seconds) (t/new-duration 20 :seconds)))
    (is (t/< (t/new-duration 10 :seconds) (t/new-duration 20 :seconds)))
    (is (t/<= (t/new-duration 20 :seconds) (t/new-duration 20 :seconds)))))


(deftest comparison-test-date
  (let [t1 #inst "2019-12-24"
        t2 #inst "2019-12-31"]

    (is (t/< t1 t2))
    (is (not (t/< t1 t1)))
    (is (not (t/< t2 t1)))

    (is (t/<= t1 t2))
    (is (t/<= t1 t1))
    (is (not (t/<= t2 t1)))

    (is (not (t/> t1 t2)))
    (is (not (t/> t1 t1)))
    (is (t/> t2 t1))

    (is (not (t/>= t1 t2)))
    (is (t/>= t1 t1))
    (is (t/>= t2 t1))))

(deftest day-of-week
  (let [days (fn [strings] (map t/day-of-week strings))]
    (is (every? #{cljc.java-time.day-of-week/sunday} (days ["sun" "sunday"])))
    (is (every? #{cljc.java-time.day-of-week/monday} (days ["mon" "monday"])))
    (is (every? #{cljc.java-time.day-of-week/tuesday} (days ["tue" "tues" "tuesday"])))
    (is (every? #{cljc.java-time.day-of-week/wednesday} (days ["wed" "weds" "wednesday"])))
    (is (every? #{cljc.java-time.day-of-week/thursday} (days ["thur" "thurs" "thursday"])))
    (is (every? #{cljc.java-time.day-of-week/friday} (days ["fri" "friday"])))
    (is (every? #{cljc.java-time.day-of-week/saturday} (days ["sat" "saturday"])))))

(deftest month
  (let [months (fn [strings] (map t/month strings))]
    (is (every? #{cljc.java-time.month/january} (months ["jan" "january"])))
    (is (every? #{cljc.java-time.month/february} (months ["feb" "february"])))
    (is (every? #{cljc.java-time.month/march} (months ["mar" "march"])))
    (is (every? #{cljc.java-time.month/april} (months ["apr" "april"])))
    (is (every? #{cljc.java-time.month/may} (months ["may"])))
    (is (every? #{cljc.java-time.month/june} (months ["jun" "june"])))
    (is (every? #{cljc.java-time.month/july} (months ["jul" "july"])))
    (is (every? #{cljc.java-time.month/august} (months ["aug" "august"])))
    (is (every? #{cljc.java-time.month/september} (months ["sep" "september"])))
    (is (every? #{cljc.java-time.month/october} (months ["oct" "october"])))
    (is (every? #{cljc.java-time.month/november} (months ["nov" "november"])))
    (is (every? #{cljc.java-time.month/december} (months ["dec" "december"])))))

;; Durations. Simple constructors to create durations of specific
;; units.

(deftest duration-test
  (is (= (t/new-duration 1e6 :nanos) (t/new-duration 1 :millis)))
  (is (= (t/new-duration 1e9 :nanos) (t/new-duration 1 :seconds)))
  (is (= (t/new-duration 1000 :millis) (t/new-duration 1 :seconds)))

  (is (= (t/new-duration 24 :hours) (t/duration (t/tomorrow)))))

(deftest predicates-test
  (is (true? (t/clock? (t/clock))))
  (is (true? (t/day-of-week? t/MONDAY)))
  (is (true? (t/duration? (t/new-duration 1 :minutes))))
  (is (true? (t/instant? (t/instant))))
  (is (true? (t/date? (t/today))))
  (is (true? (t/date-time? (t/at (t/today) (t/new-time 0 0)))))
  (is (true? (t/time? (t/new-time 0 0))))
  (is (true? (t/month? t/MAY)))
  (is (true? (t/offset-date-time? (t/offset-date-time))))
  (is (true? (t/period? (t/new-period 1 :weeks))))
  (is (true? (t/year? (t/year))))
  (is (true? (t/year-month? (t/year-month))))
  (is (true? (t/zone? (t/zone))))
  (is (true? (t/zone-offset? (t/zone-offset (t/zoned-date-time)))))
  (is (true? (t/zoned-date-time? (t/zoned-date-time))))
  (is (false? (t/date? 16)))
  (is (false? (t/month? 16))))

(deftest in-test
  (is (= (t/zoned-date-time "2021-04-23T11:23:24.576270-04:00[America/Toronto]")
         (t/in (t/instant "2021-04-23T15:23:24.576270Z")
               (t/zone "America/Toronto"))))
  (is (= (t/zoned-date-time "2021-04-23T11:18:46.594720-04:00[America/Toronto]")
         (t/in (t/offset-date-time "2021-04-23T13:18:46.594720-02:00")
               (t/zone "America/Toronto"))))
  (is (= (t/zoned-date-time "2021-04-23T11:18:46.594720-04:00[America/Toronto]")
         (t/in (t/zoned-date-time "2021-04-23T08:18:46.594720-07:00[America/Los_Angeles]")
               (t/zone "America/Toronto")))))
