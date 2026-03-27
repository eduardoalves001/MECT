% Modelação e Desempenho de Redes e Serviços -> Projeto 2 - Task 2

%% =========================================================
% Task 2.a – Algorithm Development

clear; clc;
load('InputDataProject2.mat');

% Pârametros
linkCapacity = 50;   % Gbps
k = 6;
timeLimit = 30;        % Tempo limite em segundos

nNodes = size(L,1);
nFlows = size(Tu,1);

fprintf('\n========== Task 2.a: Algorithm Development ==========\n');

% Determinar os caminhos de routing dos candidatos
fprintf('Determining candidate routing paths for each unicast flow:\n');
paths = cell(nFlows,1);

for f = 1:nFlows
    s = Tu(f,1);
    d = Tu(f,2);
    [p, ~] = kShortestPath(L, s, d, k);
    paths{f} = p;
    fprintf('  Flow %2d (%2d -> %2d): %d candidate paths\n', f, s, d, length(p));
end

% Desenvolver o Multi Start Hill Climbing
fprintf('\n  Multi Start Hill Climbing algorithm developed with:\n');
fprintf('  - Greedy randomized initial solutions\n');
fprintf('  - Hill climbing optimization\n');
fprintf('  - Energy minimization objective\n');
fprintf('  - Constraint: link load <= 100%%\n');
fprintf('  - Output: best solution time tracked\n');


%% =========================================================
% Task 2.b – Run Algorithm and Report Results

fprintf('\n========== Task 2.b: Running Algorithm ==========\n');

timeLimit = 30;
fprintf('Running for %d seconds with k = %d...\n\n', timeLimit, k);

% Pre-compute anycast loads (fixed shortest paths)
% Anycast traffic is not optimized, always uses shortest path
anycastLoad = zeros(nNodes);
G_anycast = graph(L, 'upper');

for f = 1:size(Ta,1)
    s = Ta(f,1);
    up = Ta(f,2);
    down = Ta(f,3);

    % Find shortest path to nearest anycast node (5 or 12)
    [path5, d5] = shortestpath(G_anycast, s, 5);
    [path12, d12] = shortestpath(G_anycast, s, 12);

    if d5 <= d12
        pathUp = path5;
    else
        pathUp = path12;
    end

    % Add anycast upstream traffic
    for k_idx = 1:length(pathUp)-1
        i = pathUp(k_idx);
        j = pathUp(k_idx+1);
        anycastLoad(i,j) = anycastLoad(i,j) + up;
    end

    % Add anycast downstream traffic
    for k_idx = 1:length(pathUp)-1
        i = pathUp(k_idx);
        j = pathUp(k_idx+1);
        anycastLoad(j,i) = anycastLoad(j,i) + down;
    end
end

bestEnergy = inf;
bestSol = [];
bestTime = 0;

tStart = tic;

while toc(tStart) < timeLimit

    % Solução inicial do Greedy randomized
    sol0 = greedyRandomInitialSolution(paths);

    % Hill Climbing (Task 2 - focado no objetivo da energia)
    [sol, energy] = hillClimbingEnergyTask2( ...
        sol0, paths, Tu, L, nNodes, anycastLoad);

    % Dar update ao melhor global
    if energy < bestEnergy
        bestEnergy = energy;
        bestSol = sol;
        bestTime = toc(tStart);
    end
end

% Avaliação final
linkLoads = computeLinkLoads(bestSol, paths, Tu, nNodes);

% Add anycast loads
linkLoads = linkLoads + anycastLoad;

worstLinkLoad = max(linkLoads(:)) / linkCapacity;

[totalEnergy, sleepingLinks] = computeNetworkEnergy(linkLoads, L);

% Resultados
fprintf('========== Task 2.b Results ==========\n');
fprintf('Worst link load      : %.4f (%.2f%%) = %.2f Gbps\n', worstLinkLoad, worstLinkLoad*100, worstLinkLoad*linkCapacity);
fprintf('Network energy (W)   : %.2f\n', totalEnergy);
fprintf('Sleeping links       : %d\n', size(sleepingLinks,1));
fprintf('Best solution time(s): %.2f\n', bestTime);

% Monstrar links em modo adormecido
if ~isempty(sleepingLinks)
    fprintf('\nLinks in sleeping mode:\n');
    for i = 1:size(sleepingLinks,1)
        fprintf('  Link %2d - %2d\n', sleepingLinks(i,1), sleepingLinks(i,2));
    end
end

% Guardar resultados para comparação (2.a and 2.b partilham o mesmo resultado)
task2a_worstLoad = worstLinkLoad;
task2a_energy = totalEnergy;
task2a_sleepingLinks = size(sleepingLinks,1);
task2a_bestTime = bestTime;

task2b_worstLoad = worstLinkLoad;
task2b_energy = totalEnergy;
task2b_sleepingLinks = size(sleepingLinks,1);
task2b_bestTime = bestTime;

% Guardar em struct para comparar com a task3 mais tarde
task2b.worstLoad = worstLinkLoad;
task2b.energy = totalEnergy;
task2b.sleepingLinks = size(sleepingLinks,1);
task2b.bestTime = bestTime;


%% =========================================================
% Task 2.c – Run with All Possible Paths

fprintf('\n\n========== Task 2.c: All Possible Paths ==========\n');
fprintf('Computing all possible candidate paths...\n');

% Computar todos os caminhos possíveis
kAll = 100;  % Valor de K grande o suficiente para obter todos os caminhos individuais
pathsAll = cell(nFlows,1);

for f = 1:nFlows
    s = Tu(f,1);
    d = Tu(f,2);
    [p, ~] = kShortestPath(L, s, d, kAll);
    pathsAll{f} = p;
    fprintf('  Flow %d (%d->%d): %d paths\n', f, s, d, length(p));
end

fprintf('\nRunning algorithm for %d seconds...\n', timeLimit);

% Começa com o caminho mais curto (greedy) solução como melhor inicial.
bestSol = ones(nFlows, 1);  % Selecionar primeiro o (mais curto) caminho para cada flow
[bestEnergy, feasible] = evaluateEnergyTask2(bestSol, pathsAll, Tu, L, nNodes, anycastLoad);
if ~feasible
    bestEnergy = inf;
    bestSol = [];
end
bestTime = 0;
iterations = 0;
feasibleCount = feasible;

tStart = tic;

while toc(tStart) < timeLimit

    % Greedy randomized initial solution
    sol0 = zeros(nFlows, 1);
    for f = 1:nFlows
        % Bias towards first (shortest) paths: 70% chance for first 3 paths
        if rand() < 0.7 && length(pathsAll{f}) >= 3
            sol0(f) = randi(min(3, length(pathsAll{f})));
        else
            sol0(f) = randi(length(pathsAll{f}));
        end
    end

    % Hill Climbing (Task 2 - Objetivo da energia)
    [sol, energy] = hillClimbingEnergyTask2( ...
        sol0, pathsAll, Tu, L, nNodes, anycastLoad);

    % Atualizar o melhor global
    if ~isinf(energy) && energy > 0
        feasibleCount = feasibleCount + 1;
        if energy < bestEnergy
            bestEnergy = energy;
            bestSol = sol;
            bestTime = toc(tStart);
        end
    end

    iterations = iterations + 1;
end

fprintf('Total iterations: %d (feasible: %d)\n\n', iterations, feasibleCount);

% Verificar se foi encontrada uma solução válida
if isempty(bestSol)
    fprintf('WARNING: No valid solution found! Using last feasible solution...\n');
    % Tentar encontrar uma solução válida
    for attempt = 1:100
        sol0 = greedyRandomInitialSolution(pathsAll);
        [sol, energy] = hillClimbingEnergyTask2(sol0, pathsAll, Tu, L, nNodes, anycastLoad);
        if ~isinf(energy) && energy > 0
            bestSol = sol;
            bestEnergy = energy;
            break;
        end
    end
end

% Avaliação final
linkLoads = computeLinkLoads(bestSol, pathsAll, Tu, nNodes);

% Add anycast loads
linkLoads = linkLoads + anycastLoad;

worstLinkLoad = max(linkLoads(:)) / linkCapacity;

[totalEnergy, sleepingLinks] = computeNetworkEnergy(linkLoads, L);

% Resultados
fprintf('========== Task 2.c Results ==========\n');
fprintf('Worst link load      : %.4f (%.2f%%) = %.2f Gbps\n', worstLinkLoad, worstLinkLoad*100, worstLinkLoad*linkCapacity);
fprintf('Network energy (W)   : %.2f\n', totalEnergy);
fprintf('Sleeping links       : %d\n', size(sleepingLinks,1));
fprintf('Best solution time(s): %.2f\n', bestTime);

% Mostrar links que estão adormecidos
if ~isempty(sleepingLinks)
    fprintf('\nLinks in sleeping mode:\n');
    for i = 1:size(sleepingLinks,1)
        fprintf('  Link %2d - %2d\n', sleepingLinks(i,1), sleepingLinks(i,2));
    end
end

% Guardar os resultados da Task 2.c
task2c_worstLoad = worstLinkLoad;
task2c_energy = totalEnergy;
task2c_sleepingLinks = size(sleepingLinks,1);
task2c_bestTime = bestTime;

% Guadar em formato de struct para comparar com os resultados da Task3
task2c.worstLoad = worstLinkLoad;
task2c.energy = totalEnergy;
task2c.sleepingLinks = size(sleepingLinks,1);
task2c.bestTime = bestTime;

% Comparação
fprintf('\n========== Comparison: Task 2.b vs 2.c ==========\n');
fprintf('%-25s | %-15s | %-15s\n', 'Metric', 'Task 2.b', 'Task 2.c');
fprintf('%s\n', repmat('-', 1, 58));
fprintf('%-25s | %10.4f (%4.1f%%) | %10.4f (%4.1f%%)\n', ...
    'Worst Link Load', task2b_worstLoad, task2b_worstLoad*100, task2c_worstLoad, task2c_worstLoad*100);
fprintf('%-25s | %14.2f | %14.2f\n', ...
    'Network Energy (W)', task2b_energy, task2c_energy);
fprintf('%-25s | %14d | %14d\n', ...
    'Sleeping Links', task2b_sleepingLinks, task2c_sleepingLinks);
fprintf('%-25s | %14.2f | %14.2f\n', ...
    'Best Solution Time (s)', task2b_bestTime, task2c_bestTime);
