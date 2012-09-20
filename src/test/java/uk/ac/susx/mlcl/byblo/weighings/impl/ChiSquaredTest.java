package uk.ac.susx.mlcl.byblo.weighings.impl;

/**
 * Created with IntelliJ IDEA.
 * User: hamish
 * Date: 13/09/2012
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
public class ChiSquaredTest extends AbstractContextualWeightingTest<ChiSquared> {
    @Override
    protected String getWeightingName() {
        return "chisq";
    }

    @Override
    protected Class<? extends ChiSquared> getImplementation() {
        return ChiSquared.class;
    }
}