package com.work.libtest;

//import com.work.libtest.HoleID.Hole;
//import com.work.libtest.OperatorID.Operator;
import com.work.libtest.SurveyOptions.SurveyOptions;

public class Survey {
//    Hole hole;
//    Operator operator;
    //will need to add more data about the actual measuerment stuff

//    public Survey(Hole pHole, Operator pOperator) {
//        hole = pHole;
//        operator = pOperator;
//    }
//
//    //accessors
//    public Hole getHole() {
//        return hole;
//    }
//
//    public Operator getOperator() {
//        return  operator;
//    }
//
//    //mutators
//    public void setHole(Hole pHole) {
//        hole = pHole;
//    }
//
//    public void setOperator(Operator pOperator) {
//        operator = pOperator;
//    }



    SurveyOptions surveyOptions;

    public Survey(SurveyOptions pSurveyOptions) {
        surveyOptions = pSurveyOptions;
    }

    //accessors
    public SurveyOptions getSurveyOptions() {
        return surveyOptions;
    }

    //mutators
    public void setSurveyOptions(SurveyOptions pSurveyOptions) {
        surveyOptions = pSurveyOptions;
    }
}
