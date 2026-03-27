function [bestSol, bestEnergy] = hillClimbingEnergyTask2(sol, paths, Tu, L, nNodes, anycastLoad)
% Hill Climbing for Task 2 (energy minimization)
% anycastLoad is optional - if not provided, defaults to zero

if nargin < 6
    anycastLoad = zeros(nNodes);
end

[bestEnergy, feasible] = evaluateEnergyTask2(sol, paths, Tu, L, nNodes, anycastLoad);

if ~feasible
    bestEnergy = inf;
end

bestSol = sol;
improved = true;

while improved
    improved = false;

    for f = 1:length(sol)
        for p = 1:length(paths{f})

            if p ~= bestSol(f)

                newSol = bestSol;
                newSol(f) = p;

                [energy, feasible] = evaluateEnergyTask2(newSol, paths, Tu, L, nNodes, anycastLoad);

                if feasible && energy < bestEnergy
                    bestSol = newSol;
                    bestEnergy = energy;
                    improved = true;
                end
            end
        end
    end
end
end
