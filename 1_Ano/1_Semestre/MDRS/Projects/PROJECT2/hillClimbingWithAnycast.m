function [bestSol, bestCost] = hillClimbingWithAnycast(sol, paths, Tu, L, nNodes, anycastLoad)
% Hill Climbing optimization considering fixed anycast traffic
% Enforces capacity constraint of 50 Gbps including anycast loads

CAPACITY = 50; % Gbps per link direction

bestSol = sol;
bestCost = evaluateWorstLinkLoadWithAnycast(sol, paths, Tu, L, nNodes, anycastLoad);

improved = true;

while improved
    improved = false;

    for f = 1:length(sol)
        for p = 1:length(paths{f})

            if p ~= bestSol(f)

                newSol = bestSol;
                newSol(f) = p;

                cost = evaluateWorstLinkLoadWithAnycast(newSol, paths, Tu, L, nNodes, anycastLoad);

                % Only accept if improves cost AND respects capacity
                if cost < bestCost && cost <= CAPACITY
                    bestSol = newSol;
                    bestCost = cost;
                    improved = true;
                end
            end
        end
    end
end
end
