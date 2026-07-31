/* eslint-disable react-hooks/set-state-in-effect */
/**
 * 職能資格管理ページ
 */

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { skillGradeApi } from '../features/master/masterApi';
import type {
    SkillGrade,
    UpdateSkillGradeRequest,
} from '../features/master/types';
import { ROUTES } from '../constants/routes';
import { MasterSelector } from '../components/masterPage/MasterSelector';
import { MasterTable } from '../components/masterPage/MasterTable';
import { MasterButtonArea } from '../components/masterPage/MasterButonArea';
import {
    canDeleteHistory,
    canUpdate,
    isActiveHistory,
    sortMasterHistory,
} from '../components/masterPage/MasterUtils';
import { MASTER_MESSAGES } from '../features/master/messages';

export default function SkillGradePage() {

    const navigate = useNavigate();
    const [skillGrades, setSkillGrades]
        = useState<SkillGrade[]>([]);
    const [keyword, setKeyword]
        = useState('');
    const [selected, setSelected]
        = useState<SkillGrade | null>(null);
    const [skillGrade, setSkillGrade]
        = useState<number>(0);
    const [allowance, setAllowance]
        = useState('');
    const [startDate, setStartDate]
        = useState('');
    const [endDate, setEndDate]
        = useState('');
    const [message, setMessage]
        = useState('');

    const loadSkillGrades = async () => {
        try {
            const data = await skillGradeApi.findAll();
            setSkillGrades(data);

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.dataFetchFailed
            );
        }
    };
    
    useEffect(() => {
        loadSkillGrades();
    }, []);

    const filteredSkillGrades =
        useMemo(() => {
            const filtered =
                !keyword.trim()
                    ? skillGrades
                    : skillGrades.filter(
                        row => String(row.skillGrade)
                            .includes(keyword),
                    );

            return sortMasterHistory(
                filtered,
                row => row.skillGrade,
            );

        }, [skillGrades, keyword]);

    const handleSelect = (
        row: SkillGrade,
    ) => {
        setSelected(row);
        setSkillGrade(row.skillGrade);
        setAllowance(String(row.allowance));
        setStartDate(row.startDate);
        setEndDate(row.endDate ?? '');
        setMessage('');
    };

    const handleUpdate = async () => {
        if (!selected) {
            return;
        }

        try {
            const request:
                UpdateSkillGradeRequest = {
                skillGrade,
                allowance: Number(allowance),
                startDate,
            };
            await skillGradeApi
                .updateSkillGrade(request);
            await loadSkillGrades();
            setMessage(MASTER_MESSAGES.updateSuccess);
        } catch (error) {

            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.updateFailed,
            );
        }
    };

    const handleDelete = async () => {
        if (!selected) {
            return;
        }

        if (!window.confirm(MASTER_MESSAGES.deleteConfirm)) {
            return;
        }

        try {
            await skillGradeApi
                .deleteSkillGrade(
                    selected.skillGrade,
                    selected.startDate,
                );
            await loadSkillGrades();
            setSelected(null);
            setSkillGrade(0);
            setAllowance('');
            setStartDate('');
            setEndDate('');

            setMessage(
                MASTER_MESSAGES.deleteSuccess,
            );

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.deleteFailed,
            );
        }
    };

    const handleClear = () => {
        setKeyword('');
        setSelected(null);
        setSkillGrade(0);
        setAllowance('');
        setStartDate('');
        setEndDate('');
        setMessage('');
    };

    return (

        <div className="department-page">
            <div className="page-header">
                <h1>
                    マスタデータ管理画面
                </h1>
            </div>

            <div className="search-area">
                <div className="search-item">
                    <label>
                        管理対象マスタ
                    </label>

                    <MasterSelector value="skillgrade"/>
                </div>

                <div className="search-item">
                    <label>検索条件</label>

                    <input
                        value={keyword}
                        placeholder="等級で検索"
                        onChange={e =>
                            setKeyword(
                                e.target.value,
                            )
                        }
                    />
                </div>
            </div>

            <div className="content-area">
                <div className="master-panel">
                    <div className="panel-header">
                        マスター一覧
                    </div>

                    <div className="panel-body">
                        <MasterTable
                            rows={filteredSkillGrades}
                            onSelect={handleSelect}
                            columns={[
                                {
                                    header: '等級',
                                    render: row =>
                                        row.skillGrade,
                                },
                                {
                                    header: '職能資格給',
                                    render: row =>
                                        row.allowance,
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
                            <label>等級</label>
                            <input
                                value={skillGrade}
                                readOnly
                            />

                        </div>

                        <div className="form-row">
                            <label>職能資格給</label>
                            <input
                                type="number"
                                min="0"
                                value={allowance}
                                onChange={e =>
                                    setAllowance(
                                        e.target.value,
                                    )
                                }
                            />

                        </div>

                        <div className="form-row">
                            <label>適用開始日</label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={e =>
                                    setStartDate(
                                        e.target.value,
                                    )
                                }
                            />

                        </div>

                        <div className="form-row">
                            <label>適用終了日</label>
                            <input
                                value={endDate}
                                readOnly
                            />

                        </div>

                        <MasterButtonArea
                            onCreate={() => {}}
                            onUpdate={handleUpdate}
                            onDelete={handleDelete}
                            onClear={handleClear}
                            onBack={() =>
                                navigate(ROUTES.menu)
                            }
                            createDisabled={true}
                            updateDisabled={
                                !selected ||
                                !canUpdate(
                                    String(skillGrade),
                                    allowance,
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