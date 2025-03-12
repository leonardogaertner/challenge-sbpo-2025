package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // 10 minutes in milliseconds

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
    }

    public ChallengeSolution solve(StopWatch stopWatch) {
        Set<Integer> selectedOrders = new HashSet<>();
        Set<Integer> selectedAisles = new HashSet<>();
        int totalUnitsPicked = 0;

        // We will track the total available units in each aisle to avoid picking the same item too much.
        int[] totalUnitsAvailable = new int[nItems];

        // Greedily select orders and aisles
        for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
            Map<Integer, Integer> order = orders.get(orderIndex);
            int orderTotal = order.values().stream().mapToInt(Integer::intValue).sum();

            // If adding this order exceeds the upper bound, skip the order
            if (totalUnitsPicked + orderTotal > waveSizeUB) {
                continue;  // Skip this order if it can't be fit within the wave size
            }

            // Add this order to the selected set
            selectedOrders.add(orderIndex);
            totalUnitsPicked += orderTotal;

            // Try to match aisles to this order
            List<Integer> aislesThatCanFulfillOrder = findAislesForOrder(order);

            // If we can match aisles, add them
            for (Integer aisleIndex : aislesThatCanFulfillOrder) {
                selectedAisles.add(aisleIndex);
                // Mark the aisle as "used" by updating the available units
                Map<Integer, Integer> aisle = aisles.get(aisleIndex);
                for (Map.Entry<Integer, Integer> entry : aisle.entrySet()) {
                    totalUnitsAvailable[entry.getKey()] += entry.getValue();
                }
            }

            // Check if we have exceeded the upper limit of the wave size
            if (totalUnitsPicked > waveSizeUB) {
                // Backtrack if necessary by removing the last order and aisles if we exceed the wave size
                selectedOrders.remove(orderIndex);
                totalUnitsPicked -= orderTotal;

                // Remove aisles for this order
                for (Integer aisleIndex : aislesThatCanFulfillOrder) {
                    selectedAisles.remove(aisleIndex);
                }
            }

            // Check if we have satisfied the lower bound of the wave size
            if (totalUnitsPicked >= waveSizeLB && totalUnitsPicked <= waveSizeUB) {
                break;  // We have found a valid wave, we can stop
            }
        }

        // If we don't meet the lower bound after trying to select orders, return null
        if (totalUnitsPicked < waveSizeLB) {
            System.out.println("Solution doesn't meet the lower bound for total units picked.");
            return null;
        }

        // Return the solution
        return new ChallengeSolution(selectedOrders, selectedAisles);
    }

    // Helper method to find aisles that can fulfill the given order
    private List<Integer> findAislesForOrder(Map<Integer, Integer> order) {
        List<Integer> aislesThatCanFulfillOrder = new ArrayList<>();
        for (int aisleIndex = 0; aisleIndex < aisles.size(); aisleIndex++) {
            Map<Integer, Integer> aisle = aisles.get(aisleIndex);
            if (isAisleCompatible(aisle, order)) {
                aislesThatCanFulfillOrder.add(aisleIndex);
            }
        }
        return aislesThatCanFulfillOrder;
    }

    // Helper method to check if the aisle has enough stock to fulfill the order
    private boolean isAisleCompatible(Map<Integer, Integer> aisle, Map<Integer, Integer> order) {
        for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
            Integer itemIndex = entry.getKey();
            Integer requiredQuantity = entry.getValue();
            if (!aisle.containsKey(itemIndex) || aisle.get(itemIndex) < requiredQuantity) {
                return false;
            }
        }
        return true;
    }

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
