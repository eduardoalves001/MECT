function [sol, Loads, maxLoad, linkEnergy] = greedyRandomizedStrategy(nNodes, Links, T, sP, nSP, L, capacity)
% Greedy Randomized:
% - Escolhe uma ordem aleatória de fluxos
% - Para cada fluxo, testa todos os seus paths candidatos e escolhe o que minimiza worst link load

    nFlows = size(T, 1);
    randFlows = randperm(nFlows);
    sol = zeros(1, nFlows);

    best_Loads = [];
    best_energy = inf;

    for flow = randFlows
        best_maxLoad = inf;
        best_path = 0;

        for path = 1:nSP(flow)
            sol(flow) = path;

            [LoadsTmp, energyTmp] = calculateLinkLoadEnergy(nNodes, Links, T, sP, sol, L, capacity);
            maxLoadTmp = max(max(LoadsTmp(:, 3:4)));

            if maxLoadTmp < best_maxLoad
                best_maxLoad = maxLoadTmp;
                best_path = path;
                best_Loads = LoadsTmp;
                best_energy = energyTmp;
            end
        end

        sol(flow) = best_path;
    end

    Loads = best_Loads;
    maxLoad = max(max(Loads(:, 3:4)));
    linkEnergy = best_energy;
end