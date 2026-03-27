% --- Task 3.b ---
clear; close all; clc
load('InputDataProject2.mat');

nNodes = size(Nodes,1);
nc = 500;
k = 6;
anycastNodes = [5 12];

% unicast paths (k-shortest)
nFlowsU = size(Tu,1); 
sP_uni = cell(1,nFlowsU); nSP_uni = zeros(1,nFlowsU);
for f=1:nFlowsU
    [p,c] = kShortestPath(L, Tu(f,1), Tu(f,2), k);
    sP_uni{f} = p; nSP_uni(f) = length(c);
end

% anycast paths (closest DC)
sP_any = anycastPathsClosestDC(L, Ta, anycastNodes);


T_any4 = [Ta(:,1) zeros(size(Ta,1),1) Ta(:,2:3)];
for f=1:size(Ta,1)
    T_any4(f,2) = sP_any{f}{1}(end);
end

% juntar T e sP/nSP
T = [Tu; T_any4];
sP = [sP_uni, sP_any];
nSP = [nSP_uni, ones(1,size(Ta,1))];

tStart = tic;
timeLimit = 60;

bestEnergy = inf;
bestWorst = inf;

while toc(tStart) < timeLimit
    [sol0, cap0, Loads0, E0, W0, sleep0, up0] = ...
        greedyInit(nNodes, Links, T, sP, nSP, L, nc);

    [sol, capLink, Loads, worstLoad, energy, sleepIdx, upIdx, timeBest] = ...
        HillClimb(nNodes, Links, T, sP, nSP, sol0, cap0, L, nc, tStart);

    if energy < bestEnergy
        bestEnergy = energy;
        bestWorst = worstLoad;
        bestSol = sol;
        bestCap = capLink;
        bestLoads = Loads;
        bestSleep = sleepIdx;
        bestUp = upIdx;
        bestTime = timeBest;
    end
end

fprintf("Best E=%.2f | WorstLoad=%.2f | timeBest=%.2fs\n", bestEnergy, bestWorst, bestTime);
fprintf("Sleeping links: %d\n", numel(bestSleep));
fprintf("Upgraded links: %d\n", numel(bestUp));

%%
load('InputDataProject2.mat');

nNodes = size(Nodes,1);
nc = 500;
k = 6;

dcCandidates = [4 5 6 12 13];
pairs = nchoosek(dcCandidates,2);

globalBestE = inf;

for r = 1:size(pairs,1)
    anycastNodes = pairs(r,:);
    nFlowsU = size(Tu,1);
    sP_uni = cell(1,nFlowsU); nSP_uni = zeros(1,nFlowsU);
    for f = 1:nFlowsU
        [p,c] = kShortestPath(L, Tu(f,1), Tu(f,2), k);
        sP_uni{f} = p;
        nSP_uni(f) = length(c);
    end

    % anycast closest
    sP_any = anycastPathsClosestDC(L, Ta, anycastNodes);
    T_any4 = [Ta(:,1) zeros(size(Ta,1),1) Ta(:,2:3)];
    for f = 1:size(Ta,1)
        T_any4(f,2) = sP_any{f}{1}(end);
    end

    T  = [Tu; T_any4];
    sP = [sP_uni, sP_any];
    nSP = [nSP_uni, ones(1,size(Ta,1))];

    tStart = tic; timeLimit = 60;
    bestE = inf;

    while toc(tStart) < timeLimit
        [sol0, cap0] = greedyInit(nNodes, Links, T, sP, nSP, L, nc);

        [sol, capLink, Loads, worstLoad, energy, sleepIdx, upIdx] = ...
            HillClimb(nNodes, Links, T, sP, nSP, sol0, cap0, L, nc, tStart);

        if energy < bestE
            bestE = energy;
            bestW = worstLoad;
            bestLoads = Loads;
            bestSleep = sleepIdx;
            bestUp = upIdx;
        end
    end

    fprintf("Pair [%d %d] -> E=%.2f | W=%.2f | sleep=%d | up=%d\n", ...
        anycastNodes(1), anycastNodes(2), bestE, bestW, numel(bestSleep), numel(bestUp));

    if bestE < globalBestE
        globalBestE = bestE;
        globalBestW = bestW;
        globalBestPair = anycastNodes;
        globalBestSleep = bestSleep;
        globalBestUp = bestUp;
        globalBestLoads = bestLoads;
    end
end

fprintf("\nTask 3.d BEST pair: [%d %d]\n", globalBestPair(1), globalBestPair(2));
fprintf("E=%.2f | W=%.2f | sleep=%d | up=%d\n", globalBestE, globalBestW, numel(globalBestSleep), numel(globalBestUp));

disp("Sleeping links (a-b):");
disp(globalBestLoads(globalBestSleep,1:2));

disp("Upgraded links (a-b):");
disp(globalBestLoads(globalBestUp,1:2));
