function [sol, Loads, maxLoad, linkEnergy] = HillClimbingStrategy(nNodes, Links, T, sP, nSP, sol, Loads, linkEnergy, L, capacity)
% Hill Climbing:
% - Explora vizinhança alterando o path de 1 fluxo de cada vez
% - Objetivo: minimizar WORST LINK LOAD
% - Só aceita melhorias estritas (first/best improvement style)

    nFlows = size(T,1);

    maxLoad = max(max(Loads(:, 3:4)));
    bestLocalLoad = maxLoad;
    bestLocalLoads = Loads;
    bestLocalSol = sol;
    bestLocalEnergy = linkEnergy;

    improved = true;
    while improved
        improved = false;

        for flow = 1:nFlows
            for path = 1:nSP(flow)
                if path ~= sol(flow)
                    auxSol = sol;
                    auxSol(flow) = path;

                    [auxLoads, auxLinkEnergy] = calculateLinkLoadEnergy(nNodes, Links, T, sP, auxSol, L, capacity);
                    auxMaxLoad = max(max(auxLoads(:, 3:4)));

                    if auxMaxLoad < bestLocalLoad
                        bestLocalLoad = auxMaxLoad;
                        bestLocalLoads = auxLoads;
                        bestLocalSol = auxSol;
                        bestLocalEnergy = auxLinkEnergy;
                        improved = true;
                    end
                end
            end
        end

        if improved
            maxLoad = bestLocalLoad;
            Loads = bestLocalLoads;
            sol = bestLocalSol;
            linkEnergy = bestLocalEnergy;
        end
    end
end