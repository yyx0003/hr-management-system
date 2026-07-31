/* eslint-disable react-hooks/set-state-in-effect */
/**
 * 役職管理ページ
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { positionApi } from '../features/master/masterApi';

import type {
    Position,
    CreatePositionRequest,
    UpdatePositionRequest,
} from '../features/master/types';

import { MasterSelector } from '../components/masterPage/MasterSelector';
import { MasterTable } from '../components/masterPage/MasterTable';
import { MasterButtonArea } from '../components/masterPage/MasterButonArea';

import { ROUTES } from '../constants/routes';

import {
    canCreate,
    canDeleteHistory,
    canUpdate,
    isActiveHistory,
    sortMasterHistory,
} from '../components/masterPage/MasterUtils';
import { MASTER_MESSAGES } from '../features/master/messages';

export default function PositionPage() {

    const navigate = useNavigate();

    const [positions, setPositions]
        = useState<Position[]>([]);

    const [keyword, setKeyword]
        = useState('');

    const [selected, setSelected]
        = useState<Position | null>(null);

    const [positionName, setPositionName]
        = useState('');

    const [positionAllowance, setPositionAllowance]
        = useState('');

    const [startDate, setStartDate] = useState('');

    const [endDate, setEndDate] = useState('');

    const [message, setMessage] = useState('');

    const [isCreateMode, setIsCreateMode] = useState(false);

    const loadPositions = useCallback(async () => {
        try {
            const data = await positionApi.findAll();
            setPositions(data);
        } catch (error) {

            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.dataFetchFailed
            );
        }
    }, []);

    useEffect(() => {
        loadPositions();
    }, [loadPositions]);

    const filteredPositions =
        useMemo(() => {
            const filtered =
                !keyword.trim()
                    ? positions
                    : positions.filter( position =>
                            position.positionName.includes(
                                keyword,
                            ),
                    );

            return sortMasterHistory(
                filtered,
                row => row.positionId,
            );
        }, [positions, keyword]);

    const handleSelect = (
        position: Position,
    ) => {
        setSelected(position);
        setIsCreateMode(false);
        setPositionName(
            position.positionName,
        );

        setPositionAllowance(
            String(position.positionAllowance),
        );

        setStartDate(
            position.startDate,
        );

        setEndDate(
            position.endDate ?? '',
        );

        setMessage('');
    };

    const handleNew = () => {
        setSelected(null);
        setIsCreateMode(true);
        setPositionName('');
        setPositionAllowance('');
        setStartDate('');
        setEndDate('');
    };

    const handleCreate = async () => {
        try {
            const request:
                CreatePositionRequest = {
                positionName,
                positionAllowance: 
                    Number(positionAllowance),
                startDate,

            };
            await positionApi.createPosition(
                request);

            await loadPositions();
            setMessage(MASTER_MESSAGES.createSuccess);
            handleNew();

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.createFailed
            );
        }
    };

    const handleUpdate = async () => {
        if (!selected) {
            return;
        }
        try {

            const request:
                UpdatePositionRequest = {
                positionId: selected.positionId,
                positionName,
                positionAllowance:
                    Number(positionAllowance),
                startDate
            };

            await positionApi.updatePosition(
                request
            );
            await loadPositions();
            setMessage(MASTER_MESSAGES.updateSuccess);

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.updateFailed
            );
        }
    };

    const handleDelete = async () => {
        if (!selected) {
            return;
        }
        if (
            !window.confirm(
                MASTER_MESSAGES.deleteConfirm
            )
        ) {
            return;
        }
        try {
            await positionApi.deletePosition(
                selected.positionId,
                selected.startDate,
            );
            await loadPositions();
            handleNew();
            setMessage(
                MASTER_MESSAGES.deleteSuccess
            );

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.deleteFailed
            );
        }
    };

    const handleClear = () => {
        setKeyword('');
        setPositionName('');
        setPositionAllowance('');
        setStartDate('');
        setEndDate('');
        setSelected(null);
        setIsCreateMode(false);
        setMessage('');
    };

    return (
        <div className="department-page">
            <div className="page-header">
                <h1>マスタデータ管理画面</h1>
            </div>

            <div className="search-area">
                <div className="search-item">
                    <label>
                        管理対象マスタ
                    </label>

                    <MasterSelector
                        value="position"
                    />
                </div>

                <div className="search-item">
                    <label>
                        検索条件
                    </label>
                    <input
                        value={keyword}
                        placeholder="名称で検索"
                        onChange={e =>
                            setKeyword(
                                e.target.value,
                            )
                        }
                    />
                </div>

                <button onClick={handleNew}>
                    新規作成
                </button>

            </div>

            <div className="content-area">
                <div className="master-panel">
                    <div className="panel-header">
                        マスター一覧
                    </div>

                    <div className="panel-body">
                        <MasterTable
                            rows={filteredPositions}
                            onSelect={
                                handleSelect
                            }
                            columns={[
                                {
                                    header: 'ID',
                                    render: row =>
                                        row.positionId,
                                },
                                {
                                    header: '役職名',
                                    render: row =>
                                        row.positionName,
                                },
                                {
                                    header: '手当額',
                                    render: row =>
                                        row.positionAllowance,
                                },
                                {
                                    header: '適用期間',
                                    render: row =>
                                        `${row.startDate} ～ ${row.endDate ?? ''}`,
                                },
                                {
                                    header: '状態',
                                    render: row =>
                                        isActiveHistory(
                                            row.startDate,
                                            row.endDate,
                                        )
                                            ? '有効'
                                            : '無効',
                                },
                            ]}
                        />
                    </div>
                </div>

                <div className="master-panel">
                    <div className="panel-header">
                        詳細入力エリア
                    </div>

                    <div className="panel-body">
                        <div className="form-row">
                            <label>
                                役職名
                            </label>
                            <input
                                value={positionName}
                                onChange={e =>
                                    setPositionName(
                                        e.target.value,
                                    )
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label>
                                手当額
                            </label>

                            <input
                                type="number"
                                min="0"
                                value={positionAllowance}
                                onChange={e =>
                                    setPositionAllowance(
                                        e.target.value
                                    )
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label>
                                適用開始日
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={e =>
                                    setStartDate(e.target.value)
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label>
                                適用終了日
                            </label>

                            <input
                                value={endDate}
                                readOnly
                            />
                        </div>

                        <MasterButtonArea
                            onCreate={handleCreate}
                            onUpdate={handleUpdate}
                            onDelete={handleDelete}
                            onClear={handleClear}
                            onBack={() =>
                                navigate(ROUTES.menu)
                            }
                            createDisabled={
                                !isCreateMode ||
                                !canCreate(
                                    positionName,
                                    positionAllowance,
                                    startDate
                                )
                            }
                            updateDisabled={
                                !selected ||
                                !canUpdate(
                                    positionName,
                                    positionAllowance,
                                    startDate,
                                    endDate
                                )
                            }
                            deleteDisabled={
                                !selected ||
                                !canDeleteHistory(
                                    selected.startDate,
                                    selected.endDate,
                                )
                            }
                        />
                    </div>
                </div>
            </div>

            <div className="message-area">
                {message}
            </div>

        </div>
    );
}