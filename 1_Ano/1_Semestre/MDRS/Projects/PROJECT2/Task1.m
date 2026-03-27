% Modelação e Desempenho de Redes e Serviços -> Projeto 2 - Task 1

%% Task 1.a - Shortest Path Routing

% Tráfego Unicast: fluxos entre nós específicos (origem → destino)
% Tráfego Anycast: fluxos onde o destino é o nó anycast mais próximo (nós 5 e 12)

clear;
clc;

% Carregar dados de input
load('InputDataProject2.mat');

% L  -> Matriz de comprimento entre Links (Km)
% Tu -> Unicast flows: [src dst up down] (Gbps)
% Ta -> Anycast flows: [src up down] (Gbps)

nNodes = size(L,1);
C = 50;                 % Capacidade dos Links (Na task 1 e 2 é pedido que seja 50) (Gbps)
anycastNodes = [5 12];  % Nós que permitem tráfego Anycast são segundo o enunciado o 5 e o 12.

% Construir o grafo (Usando os comprimentos dos Links)
% L matrix uses Inf for non-existent links, finite values for actual links
G = graph(L, 'upper');

% Inicializar matrizes de carga direcionais dos links (Gbps)
% linkLoad(i,j) = tráfego de i para j
linkLoad = zeros(nNodes);

% Tráfego Unicast (Caminho mais curto)
% s = origem; d= destino, up = tráfego de s para d, down = tráfego de d para s
% Links bidirecionais: cada direção tem capacidade independente de 50 Gbps

for f = 1:size(Tu,1)
    s = Tu(f,1);
    d = Tu(f,2);
    up = Tu(f,3);
    down = Tu(f,4);

    % Caminho de s para d (tráfego up)
    path = shortestpath(G, s, d);
    for k = 1:length(path)-1
        i = path(k);
        j = path(k+1);
        linkLoad(i,j) = linkLoad(i,j) + up;
    end

    % Caminho de d para s (tráfego down) - mesmo caminho, direção oposta
    for k = 1:length(path)-1
        i = path(k);
        j = path(k+1);
        linkLoad(j,i) = linkLoad(j,i) + down;
    end
end

% Tráfego Anycast (Caminho mais curto para o nó anycast mais próximo)
% s = origem, up = tráfego de s para anycast, down = tráfego de anycast para s
for f = 1:size(Ta,1)
    s = Ta(f,1);
    up = Ta(f,2);
    down = Ta(f,3);

    % Encontrar o nó anycast mais próximo (5 ou 12)
    [path5, dist5] = shortestpath(G, s, 5);
    [path12, dist12] = shortestpath(G, s, 12);

    % Escolher o caminho mais curto
    if dist5 <= dist12
        pathUp = path5;
    else
        pathUp = path12;
    end

    % Caminho de s para anycast (tráfego up)
    for k = 1:length(pathUp)-1
        i = pathUp(k);
        j = pathUp(k+1);
        linkLoad(i,j) = linkLoad(i,j) + up;
    end

    % Caminho de anycast para s (tráfego down) - mesmo caminho, direção oposta
    for k = 1:length(pathUp)-1
        i = pathUp(k);
        j = pathUp(k+1);
        linkLoad(j,i) = linkLoad(j,i) + down;
    end
end

% Computar as cargas dos links e a pior carga (considerando ambas as direções)
linkLoadVec = [];
linkIndex = [];

for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))  % Only actual physical links (L < Inf)
            % Para links bidirecionais, consideramos o máximo entre as duas direções
            maxLoad = max(linkLoad(i,j), linkLoad(j,i));
            linkLoadVec(end+1) = maxLoad;
            linkIndex(end+1,:) = [i j];
        end
    end
end

[worstLinkLoad, idx] = max(linkLoadVec);
worstUtilization = worstLinkLoad / C * 100;


% Resultados
fprintf('--- Task 1.a Results ---\n');
fprintf('Worst link load: %.2f Gbps\n', worstLinkLoad);
fprintf('Worst link utilization: %.2f %%\n', worstUtilization);
fprintf('Worst link: (%d - %d)\n', linkIndex(idx,1), linkIndex(idx,2));

% Mostrar as cargas de todos os links
disp('Link loads (Gbps):');
for k = 1:length(linkLoadVec)
    fprintf('Link %2d-%2d : %6.2f Gbps\n', ...
        linkIndex(k,1), linkIndex(k,2), linkLoadVec(k));
end


%% Task 1.b - Consumo de Energia da Rede
% Usar as cargas dos Links do exercicio 1.a (incluindo unicast e anycast)

% Parâmetros
routerCapacity = 500;  % De acordo com o enunciado (Consider that each router has a capacity of 500 Gbps)
nNodes = size(linkLoad,1); % Número de Nós na rede


% Consumo de Energia do Router
routerLoad = zeros(nNodes,1);

% Somar todo o tráfego que passa por cada Router
% Use only outgoing traffic to avoid double-counting
for i = 1:nNodes
    routerLoad(i) = sum(linkLoad(i,:));
end

% Computar a Energia do Router
routerEnergy = zeros(nNodes,1);
for i = 1:nNodes
    t = routerLoad(i) / routerCapacity;
    routerEnergy(i) = 10 + 90 * t^2;
end

totalRouterEnergy = sum(routerEnergy);

% Consumo de Energia dos Links
linkEnergy = 0;
sleepingLinks = [];
TOLERANCE = 1e-9; % Threshold for considering a link as having no traffic

for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))   % Link Existente (finite distance)
            % Link ativo se houver tráfego em qualquer direção
            if linkLoad(i,j) > TOLERANCE || linkLoad(j,i) > TOLERANCE
                % Link ativo (50 Gbps)
                El = 6 + 0.2 * L(i,j);
            else
                % Link em modo adormecido
                El = 2;
                sleepingLinks(end+1,:) = [i j];
            end

            linkEnergy = linkEnergy + El;
        end
    end
end

% Energia total da Rede
totalEnergy = totalRouterEnergy + linkEnergy;

% Count active links for verification
numActiveLinks = 0;
numPhysicalLinks = 0;
for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))
            numPhysicalLinks = numPhysicalLinks + 1;
            if linkLoad(i,j) > TOLERANCE || linkLoad(j,i) > TOLERANCE
                numActiveLinks = numActiveLinks + 1;
            end
        end
    end
end

% Resultados
fprintf('\n--- Task 1.b Results ---\n');
fprintf('Total router energy: %.2f\n', totalRouterEnergy);
fprintf('Total link energy: %.2f\n', linkEnergy);
fprintf('Total network energy: %.2f\n', totalEnergy);
fprintf('Total physical links: %d\n', numPhysicalLinks);
fprintf('Active links: %d\n', numActiveLinks);
fprintf('Sleeping links: %d\n', size(sleepingLinks,1));

fprintf('\nFirst 10 sleeping links:\n');
if isempty(sleepingLinks)
    fprintf('None\n');
else
    numToShow = min(10, size(sleepingLinks,1));
    for k = 1:numToShow
        fprintf('Link %d-%d (loads: %.4f / %.4f)\n', sleepingLinks(k,1), sleepingLinks(k,2), ...
            linkLoad(sleepingLinks(k,1), sleepingLinks(k,2)), linkLoad(sleepingLinks(k,2), sleepingLinks(k,1)));
    end
    if size(sleepingLinks,1) > 10
        fprintf('... and %d more\n', size(sleepingLinks,1) - 10);
    end
end





%% Task 1.c - Multi Start Hill Climbing Algorithm Development
% Objetivo: Minimizar a carga do pior Link
% Usar o Yen's k-shortest path algorithm para gerar caminhos candidatos

% Esta task desenvolve o algoritmo Multi Start Hill Climbing
% O algoritmo está implementado nas seguintes funções:
%   - kShortestPath.m: Algoritmo de Yen para k caminhos mais curtos
%   - greedyRandomInitialSolution.m: Gera solução inicial aleatória
%   - hillClimbing.m: Otimização local por Hill Climbing
%   - evaluateWorstLinkLoad.m: Avalia a carga do pior link

% A execução do algoritmo é realizada na Task 1.d
fprintf('--- Task 1.c ---\n');
fprintf('Algoritmos desenvolvidos e implementados nas funções auxiliares.\n');
fprintf('Analisar hillClimbing.m, evaluateWorstLinkLoad.m, and kShortestPath.m\n');
fprintf('Algoritmo será executado na Task 1.d\n');

%% =========================
% Task 1.d – Multi Start HC
% =========================

clear;

% Carregar dados
load('InputDataProject2.mat');

nNodes = size(L,1);
nFlows = size(Tu,1);
linkCapacity = 50; % Gbps
k = 6;
timeLimit = 30; % seconds

% Build graph from L
G = graph(L, 'upper');

% Computar o k-shortest paths
paths = cell(nFlows,1);

for f = 1:nFlows
    s = Tu(f,1);
    d = Tu(f,2);
    paths{f} = kShortestPath(L, s, d, k);
end

% Pre-compute anycast loads (fixed shortest paths)
% These must be considered during optimization to respect capacity
anycastLoad = zeros(nNodes);
G_anycast = graph(L, 'upper');

for f = 1:size(Ta,1)
    s = Ta(f,1);
    up = Ta(f,2);
    down = Ta(f,3);

    % Find shortest path to nearest anycast node
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

% Multi Start Hill Climbing
bestGlobalCost = inf;
bestGlobalSol = [];
bestTime = 0;

tStart = tic;

while toc(tStart) < timeLimit

    % Solução com Greedy randomized initial
    sol0 = greedyRandomInitialSolution(paths);

    % Melhoria com o Hill climbing (considering anycast loads)
    [sol, cost] = hillClimbingWithAnycast(sol0, paths, Tu, L, nNodes, anycastLoad);

    % Verificar o melhor global
    if cost < bestGlobalCost
        bestGlobalCost = cost;
        bestGlobalSol = sol;
        bestTime = toc(tStart);
    end
end


% Avaliar solução Final
[linkLoads] = computeLinkLoads(bestGlobalSol, paths, Tu, nNodes);

% Add pre-computed anycast loads
linkLoads = linkLoads + anycastLoad;

% Computar worst link load considerando ambas as direções
worstLinkLoad = 0;
for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))  % Only actual physical links
            maxLoad = max(linkLoads(i,j), linkLoads(j,i));
            worstLinkLoad = max(worstLinkLoad, maxLoad);
        end
    end
end

[energy, sleepingLinks] = computeNetworkEnergy(linkLoads, L);

% Debug: Check if linkLoads matrix has correct zero values
numZeroLinks = 0;
for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))  % Only actual physical links
            if linkLoads(i,j) == 0 && linkLoads(j,i) == 0
                numZeroLinks = numZeroLinks + 1;
            end
        end
    end
end

fprintf('\nDEBUG: Links with exactly zero load in both directions: %d\n', numZeroLinks);
fprintf('DEBUG: Sleeping links reported by computeNetworkEnergy: %d\n', size(sleepingLinks,1));

% Count links by traffic type
unicastOnlyLinks = 0;
anycastOnlyLinks = 0;
bothTrafficLinks = 0;
totalPhysicalLinks = 0;

unicastLoads = linkLoads - anycastLoad;

for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))  % Only actual physical links
            totalPhysicalLinks = totalPhysicalLinks + 1;
            hasUnicast = (unicastLoads(i,j) > 0 || unicastLoads(j,i) > 0);
            hasAnycast = (anycastLoad(i,j) > 0 || anycastLoad(j,i) > 0);

            if hasUnicast && hasAnycast
                bothTrafficLinks = bothTrafficLinks + 1;
            elseif hasUnicast
                unicastOnlyLinks = unicastOnlyLinks + 1;
            elseif hasAnycast
                anycastOnlyLinks = anycastOnlyLinks + 1;
            end
        end
    end
end

% Resultados
fprintf('\n===== Task 1.d Results =====\n');
fprintf('Worst link load        : %.2f Gbps\n', worstLinkLoad);
fprintf('Worst link utilization : %.2f %%\n', worstLinkLoad / linkCapacity * 100);
fprintf('Network energy (W)     : %.2f\n', energy);
fprintf('Total physical links   : %d\n', totalPhysicalLinks);
fprintf('Sleeping links         : %d\n', size(sleepingLinks,1));
fprintf('Unicast-only links     : %d\n', unicastOnlyLinks);
fprintf('Anycast-only links     : %d\n', anycastOnlyLinks);
fprintf('Both traffic links     : %d\n', bothTrafficLinks);
fprintf('Best solution found at : %.2f seconds\n', bestTime);
