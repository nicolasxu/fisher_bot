package com.fisher;

import sun.security.util.Length;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by nick on 6/14/15.
 */
public class EmaFilter extends IFilter {
    public int m_period;
    public double m_alpha;
    public double m_pr;

    //m_Pr=2.0/(Length+1.0);

    public EmaFilter(int period) {
        this.m_period = period;
        this.m_alpha = 2.0;
        this.m_pr = m_alpha/(m_period + 1.0);

    }
    public void filter(ArrayList<Double> input, ArrayList<Double> output) {
        int inputSize = input.size();

        if(output == null) {
            output = new ArrayList<Double>();
        }

        if (inputSize < 0) {
            // do nothing if input is empty
            return;
        }

        for(int index = Math.max(0, output.size() - 1) ; index < inputSize; index++) {

            if(index == 0) {
                if(index >= output.size()) {
                    output.add(input.get(index));
                    System.out.println("adding " +input.get(index) +" at index: " + index);
                } else {
                    output.set(index, input.get(index));
                    System.out.println("setting " + input.get(index) + " at index: " + index);
                }
            }

            if(index > 0) {
                double result = input.get(index)*m_pr + (1 - m_pr)*output.get(index -1);

                if(index >= output.size()) {
                    output.add(result);
                } else {
                    output.set(index, result);
                }
            }
        }
    }

    public void test() {

        double[] closePrice;
        closePrice = new double[100];
        closePrice[0]=1.35456;
        closePrice[1]=1.35472;
        closePrice[2]=1.35472;
        closePrice[3]=1.35492;
        closePrice[4]=1.35489;
        closePrice[5]=1.35512;
        closePrice[6]=1.35489;
        closePrice[7]=1.35519;
        closePrice[8]=1.35493;
        closePrice[9]=1.35499;
        closePrice[10]=1.35511;
        closePrice[11]=1.35491;
        closePrice[12]=1.35489;
        closePrice[13]=1.35487;
        closePrice[14]=1.35484;
        closePrice[15]=1.35481;
        closePrice[16]=1.35462;
        closePrice[17]=1.35411;
        closePrice[18]=1.35421;
        closePrice[19]=1.35421;
        closePrice[20]=1.35392;
        closePrice[21]=1.35409;
        closePrice[22]=1.35432;
        closePrice[23]=1.35427;
        closePrice[24]=1.35436;
        closePrice[25]=1.35409;
        closePrice[26]=1.35394;
        closePrice[27]=1.35422;
        closePrice[28]=1.35379;
        closePrice[29]=1.3535;
        closePrice[30]=1.35341;
        closePrice[31]=1.35391;
        closePrice[32]=1.35394;
        closePrice[33]=1.35453;
        closePrice[34]=1.35445;
        closePrice[35]=1.35431;
        closePrice[36]=1.35459;
        closePrice[37]=1.35464;
        closePrice[38]=1.35465;
        closePrice[39]=1.35462;
        closePrice[40]=1.35456;
        closePrice[41]=1.35476;
        closePrice[42]=1.35475;
        closePrice[43]=1.35486;
        closePrice[44]=1.35474;
        closePrice[45]=1.35504;
        closePrice[46]=1.35562;
        closePrice[47]=1.35553;
        closePrice[48]=1.35548;
        closePrice[49]=1.35528;
        closePrice[50]=1.35555;
        closePrice[51]=1.35568;
        closePrice[52]=1.35542;
        closePrice[53]=1.35478;
        closePrice[54]=1.35451;
        closePrice[55]=1.35477;
        closePrice[56]=1.35478;
        closePrice[57]=1.35443;
        closePrice[58]=1.35462;
        closePrice[59]=1.3544;
        closePrice[60]=1.35473;
        closePrice[61]=1.35429;
        closePrice[62]=1.35401;
        closePrice[63]=1.35386;
        closePrice[64]=1.35396;
        closePrice[65]=1.35216;
        closePrice[66]=1.35242;
        closePrice[67]=1.35312;
        closePrice[68]=1.35292;
        closePrice[69]=1.3528;
        closePrice[70]=1.35355;
        closePrice[71]=1.35402;
        closePrice[72]=1.35419;
        closePrice[73]=1.35443;
        closePrice[74]=1.3537;
        closePrice[75]=1.35366;
        closePrice[76]=1.35321;
        closePrice[77]=1.35353;
        closePrice[78]=1.35332;
        closePrice[79]=1.35309;
        closePrice[80]=1.35307;
        closePrice[81]=1.35293;
        closePrice[82]=1.3534;
        closePrice[83]=1.35431;
        closePrice[84]=1.35608;
        closePrice[85]=1.35567;
        closePrice[86]=1.35572;
        closePrice[87]=1.35509;
        closePrice[88]=1.3548;
        closePrice[89]=1.35476;
        closePrice[90]=1.35444;
        closePrice[91]=1.35426;
        closePrice[92]=1.35406;
        closePrice[93]=1.35431;
        closePrice[94]=1.354;
        closePrice[95]=1.35377;
        closePrice[96]=1.3535;
        closePrice[97]=1.35284;
        closePrice[98]=1.35278;
        closePrice[99]=1.35302;

        ArrayList<Double> close = new ArrayList<Double>();
        for(int i = 0; i < closePrice.length; i++) {
            close.add(closePrice[i]);
        }
        ArrayList<Double> output = new ArrayList<Double>();
        this.filter(close, output);

        for(int i = 0; i < output.size(); i++) {
            System.out.println("output[" + i + "]=" + output.get(i));
        }


    }
}
