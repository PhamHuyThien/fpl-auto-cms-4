
package com.thiendz.tool.fplautocms.utils;

import java.util.ArrayList;
import java.util.List;

public class Combination {

    private final int k;
    private final int n;

    private boolean allResult;

    private List<List<Integer>> alInt;
    private boolean build;

    public Combination(int k, int n) {
        this.k = k;
        this.n = n;
        initCombination();
    }

    public Combination(int k, int n, boolean allResult) {
        this.k = k;
        this.n = n;
        this.allResult = allResult;
    }

    public List<List<Integer>> getResult() {
        build();
        return alInt;
    }

    public void build() {
        build = !build;
        initCombination();
        for (int i = k; i <= (allResult ? n : k); i++) {
            int[] arrTmp = createTmpArrCombination(i + 1);
            combination(i, n, 1, arrTmp);
        }
    }

    private void combination(int k, int n, int i, int[] arrTmp) {
        for (int j = arrTmp[i - 1] + 1; j <= n - k + i; j++) {
            arrTmp[i] = j;
            if (i == k) {
                ArrayList<Integer> alIntTmp = new ArrayList<>();
                for (int m = 1; m <= k; m++) {
                    alIntTmp.add(arrTmp[m] - 1);
                }
                this.alInt.add(alIntTmp);
            } else {
                combination(k, n, i + 1, arrTmp);
            }
        }
    }

    private void initCombination() {
        alInt = new ArrayList<>();
    }

    private static int[] createTmpArrCombination(int count) {
        int[] resInt = new int[count];
        for (int i = 0; i < count; i++) {
            resInt[i] = 0;
        }
        return resInt;
    }

}
